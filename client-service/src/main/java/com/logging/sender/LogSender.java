package com.logging.sender;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for sending logs to log collector via tcp and udp <br/>
 * 
 * Native java.net.Socket and DatagramSocket are used intentionally <br/>
 * to demonstrate low-level TCP and UDP log transport behavior. <br/>
 *
 * This approach makes protocol characteristics explicit: <br/>
 * - TCP provides reliable, stateful delivery and requires reconnection <br/>
 * - UDP is connectionless, best-effort, and does not guarantee delivery <br/>
 *
 * Higher-level frameworks (Kafka, Logback, Netty) abstract these details <br/>
 * and are intentionally avoided to keep the implementation transparent <br/>
 * and aligned with the assignment’s objectives. <br/>
 */
@Slf4j
@Component
public class LogSender {

	private static final String COLLECTOR_HOST = "localhost";
	private static final int TCP_PORT = 9090;
	private static final int UDP_PORT = 9091;

	private final Random random = new Random();

	/**
	 * To manage tcp connection life cycle
	 */
	private Socket tcpSocket;

	/**
	 * text based writer used to write logs over the socket.
	 */
	private PrintWriter tcpWriter;

	/**
	 * DatagramSocket is connectionless, it not persistent connection. It just sends logs to the local OS and succeeds
	 * as long as local OS accepts it. Delivery is not confirmed here.
	 */
	private DatagramSocket udpSocket;

	public LogSender() {
		try {
			// Initialize TCP connection
			tcpSocket = new Socket(COLLECTOR_HOST, TCP_PORT);
			tcpWriter = new PrintWriter(tcpSocket.getOutputStream(), true);

			// Initialize UDP socket
			udpSocket = new DatagramSocket();

			log.info("Log sender initialized (TCP + UDP)");
		} catch (IOException e) {
			log.error("Failed to initialize log sender: {}", e.getMessage());
		}
	}

	/**
	 * sends log to controller by randomly choosing TCP or UDP
	 * 
	 * @param logMessage
	 */
	public void sendLog(String logMessage) {
		try {
			boolean useTcp = random.nextBoolean();
			log.info("Sending via {}", useTcp ? "TCP" : "UDP");

			if (useTcp) {
				sendTcp(logMessage);
			} else {
				sendUdp(logMessage);
			}
		} catch (Exception e) {
			log.error("Failed to send log: {}", e.getMessage());
			reconnect();
		}
	}

	private void sendTcp(String logMessage) {
		if (tcpWriter == null) {
			reconnect();
		}

		if (tcpWriter != null) {
			tcpWriter.println(logMessage);
			if (tcpWriter.checkError()) {
				log.error("TCP write error, reconnecting...");
				reconnect();
			}
		}
	}

	private void sendUdp(String logMessage) throws IOException {
		if (udpSocket != null && !udpSocket.isClosed()) {
			byte[] data = logMessage.getBytes();
			InetAddress address = InetAddress.getByName(COLLECTOR_HOST);
			DatagramPacket packet = new DatagramPacket(data, data.length, address, UDP_PORT);
			udpSocket.send(packet);
		}
	}

	/**
	 * Attempts to reconnect for TCP
	 */
	private void reconnect() {
		close();
		try {
			Thread.sleep(1000); // Wait before reconnecting
			tcpSocket = new Socket(COLLECTOR_HOST, TCP_PORT);
			tcpWriter = new PrintWriter(tcpSocket.getOutputStream(), true);
			log.info("TCP reconnected");
		} catch (Exception e) {
			log.error("Reconnection failed: {}", e.getMessage());
		}
	}

	public void close() {
		try {
			if (tcpWriter != null) {
				tcpWriter.close();
			}
			if (tcpSocket != null && !tcpSocket.isClosed()) {
				tcpSocket.close();
			}
			if (udpSocket != null && !udpSocket.isClosed()) {
				udpSocket.close();
			}
		} catch (IOException e) {
			log.error("Error closing connections: {}", e.getMessage());
		}
	}
}