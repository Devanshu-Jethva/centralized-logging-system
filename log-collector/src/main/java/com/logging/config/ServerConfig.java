package com.logging.config;

import org.springframework.stereotype.Component;

import com.logging.server.TcpLogServer;
import com.logging.server.UdpLogServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerConfig {

	private final TcpLogServer tcpLogServer;

	private final UdpLogServer udpLogServer;

	@PostConstruct
	public void start() {
		// Start TCP and UDP servers asynchronously
		tcpLogServer.start();
		udpLogServer.start();

		log.info("=== Log Collector Started ===");
		log.info("TCP Server: localhost:9090");
		log.info("UDP Server: localhost:9091");
		log.info("Metrics: http://localhost:8081/metrics");
	}

	@PreDestroy
	public void stop() {
		log.info("Gracefully shutting down Log Collector...");
		tcpLogServer.stop();
		udpLogServer.stop();
	}

}
