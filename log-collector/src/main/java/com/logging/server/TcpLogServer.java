package com.logging.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.logging.processor.LogProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpLogServer {

	private static final int TCP_PORT = 9090;
	private static final int THREAD_POOL_SIZE = 20;

	private final LogProcessor logProcessor;
	private final Scheduler logProcessingScheduler;

	private ServerSocket serverSocket;
	private ExecutorService executorService;
	private volatile boolean running = false;

	public void start() {
		executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, runnable -> {
			// the factory to use when creating new threads
			Thread thread = new Thread(runnable);
			thread.setName("tcp-worker-" + thread.threadId());
			thread.setDaemon(true);
			return thread;
		});

		running = true;

		new Thread(() -> {
			try {
				serverSocket = new ServerSocket(TCP_PORT);
				log.info("TCP Server listening on port {}", TCP_PORT);

				while (running) {
					try {
						/**
						 * Only one TCP connection is made from client-service and persisted, so in while loop this line
						 * is executed once, it will accept the connection and pass to executorService. When it runs
						 * again in while loop it will be blocked waiting for new TCP connection so at max one TCP
						 * connection persisted and processed
						 */
						Socket clientSocket = serverSocket.accept();

						/**
						 * clientSocket is passed to new task to executorService, so will close this socket only in
						 * handleClient
						 */
						executorService.submit(() -> handleClient(clientSocket));
					} catch (IOException e) {
						if (running) {
							log.error("Error accepting TCP connection: {}", e.getMessage());
						}
					}
				}
			} catch (IOException e) {
				log.error("Failed to start TCP server: {}", e.getMessage());
			}
		}, "tcp-acceptor").start();
	}

	/**
	 * Socket connection is made from client-service and persisted over there so here we are continuously reading
	 * inputStream for logs
	 * 
	 * @param socket
	 */
	private void handleClient(Socket socket) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String logMessage = line;

				// Process asynchronously with backpressure
				Mono.fromRunnable(() -> logProcessor.processLog(logMessage)).subscribeOn(logProcessingScheduler)
						.subscribe(null, error -> log.error("Error processing TCP log: {}", error.getMessage()));
			}
		} catch (IOException e) {
			log.error("Error handling TCP client: {}", e.getMessage());
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	public void stop() {
		running = false;

		try {
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}
		} catch (IOException e) {
			log.error("Error closing TCP server socket: {}", e.getMessage());
		}

		if (executorService != null) {
			executorService.shutdown();
			try {
				if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
					executorService.shutdownNow();
				}
			} catch (InterruptedException e) {
				executorService.shutdownNow();
			}
		}

		log.info("TCP Server stopped");
	}
}