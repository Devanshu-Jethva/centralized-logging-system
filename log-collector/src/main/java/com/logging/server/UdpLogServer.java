package com.logging.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
public class UdpLogServer {

	private static final int UDP_PORT = 9091;
	private static final int BUFFER_SIZE = 65536;
	private static final int THREAD_POOL_SIZE = 15;

	private final LogProcessor logProcessor;
	private final Scheduler logProcessingScheduler;

	private DatagramSocket socket;
	private ExecutorService executorService;
	private volatile boolean running = false;

	public void start() {
		executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, runnable -> {
			// the factory to use when creating new threads
			Thread thread = new Thread(runnable);
			thread.setName("udp-worker-" + thread.threadId());
			thread.setDaemon(true);
			return thread;
		});

		running = true;

		new Thread(() -> {
			try {
				/**
				 * The server binds to port 9091 <br/>
				 * The OS delivers incoming UDP packets for that port <br/>
				 */
				socket = new DatagramSocket(UDP_PORT);
				log.info("UDP Server listening on port " + UDP_PORT);

				byte[] buffer = new byte[BUFFER_SIZE];

				while (running) {
					try {
						/**
						 * receive() blocks until a packet arrives <br/>
						 * When a packet arrives, receive() unblocks
						 */
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						socket.receive(packet);

						String message = new String(packet.getData(), 0, packet.getLength()).trim();

						// Process in thread pool with backpressure
						executorService.submit(() -> {
							Mono.fromRunnable(() -> logProcessor.processLog(message))
									.subscribeOn(logProcessingScheduler).subscribe(null,
											error -> log.error("Error processing UDP log: {}", error.getMessage()));
						});

					} catch (Exception e) {
						if (running) {
							log.error("Error receiving UDP packet: {}", e.getMessage());
						}
					}
				}
			} catch (Exception e) {
				log.error("Failed to start UDP server: {}", e.getMessage());
			}
		}, "udp-receiver").start();
	}

	public void stop() {
		running = false;

		if (socket != null && !socket.isClosed()) {
			socket.close();
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

		log.info("UDP Server stopped");
	}
}
