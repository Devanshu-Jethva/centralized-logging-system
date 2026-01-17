package com.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.logging.entity.LogEntry;
import com.logging.service.LogStorageService;
import com.logging.service.impl.LogStorageServiceImpl;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LogStorageServiceTest {

	private LogStorageService logStorageService;

	@BeforeEach
	void setUp() {
		logStorageService = new LogStorageServiceImpl();
	}

	@Test
	void testIngestLog() {
		LogEntry logEntry = createSampleLog("linux_login", "INFO", "testuser", false);

		Mono<Void> result = logStorageService.ingestLog(logEntry);

		StepVerifier.create(result).verifyComplete();

		// Wait for async processing
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertEquals(1, logStorageService.getTotalLogs());
	}

	@Test
	void testQueryLogsByService() {
		LogEntry log1 = createSampleLog("linux_login", "INFO", "user1", false);
		LogEntry log2 = createSampleLog("windows_login", "INFO", "user2", false);
		LogEntry log3 = createSampleLog("linux_login", "ERROR", "user3", true);

		logStorageService.ingestLog(log1).block();
		logStorageService.ingestLog(log2).block();
		logStorageService.ingestLog(log3).block();

		// Wait for processing
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Flux<LogEntry> results = logStorageService.queryLogs("linux_login", null, null, null, null, null);

		StepVerifier.create(results).expectNextCount(2).verifyComplete();
	}

	@Test
	void testQueryLogsBySeverity() {
		LogEntry log1 = createSampleLog("linux_login", "INFO", "user1", false);
		LogEntry log2 = createSampleLog("linux_login", "ERROR", "user2", false);
		LogEntry log3 = createSampleLog("linux_login", "ERROR", "user3", true);

		logStorageService.ingestLog(log1).block();
		logStorageService.ingestLog(log2).block();
		logStorageService.ingestLog(log3).block();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Flux<LogEntry> results = logStorageService.queryLogs(null, "ERROR", null, null, null, null);

		StepVerifier.create(results).expectNextCount(2).verifyComplete();
	}

	@Test
	void testQueryLogsWithBlacklist() {
		LogEntry log1 = createSampleLog("linux_login", "INFO", "user1", false);
		LogEntry log2 = createSampleLog("linux_login", "INFO", "user2", true);

		logStorageService.ingestLog(log1).block();
		logStorageService.ingestLog(log2).block();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Flux<LogEntry> results = logStorageService.queryLogs(null, null, null, true, null, null);

		StepVerifier.create(results).expectNextCount(1).verifyComplete();
	}

	@Test
	void testQueryLogsWithLimit() {
		for (int i = 0; i < 10; i++) {
			LogEntry log = createSampleLog("linux_login", "INFO", "user" + i, false);
			logStorageService.ingestLog(log).block();
		}

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Flux<LogEntry> results = logStorageService.queryLogs(null, null, null, null, 5, null);

		StepVerifier.create(results).expectNextCount(5).verifyComplete();
	}

	@Test
	void testGetMetrics() {
		LogEntry log1 = createSampleLog("linux_login", "INFO", "user1", false);
		LogEntry log2 = createSampleLog("windows_login", "ERROR", "user2", false);
		LogEntry log3 = createSampleLog("linux_login", "INFO", "user3", false);

		logStorageService.ingestLog(log1).block();
		logStorageService.ingestLog(log2).block();
		logStorageService.ingestLog(log3).block();

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Mono<Map<String, Object>> metrics = logStorageService.getMetrics();

		StepVerifier.create(metrics).assertNext(m -> {
			assertEquals(3L, m.get("totalLogsReceived"));

			@SuppressWarnings("unchecked")
			Map<String, Long> categoryMap = (Map<String, Long>) m.get("logsByCategory");
			assertEquals(2L, categoryMap.get("linux_login"));
			assertEquals(1L, categoryMap.get("windows_login"));

			@SuppressWarnings("unchecked")
			Map<String, Long> severityMap = (Map<String, Long>) m.get("logsBySeverity");
			assertEquals(2L, severityMap.get("info"));
			assertEquals(1L, severityMap.get("error"));
		}).verifyComplete();
	}

	@Test
	void testBackpressure() {
		// This test simulates high load to test backpressure
		for (int i = 0; i < 2000; i++) {
			LogEntry log = createSampleLog("test", "INFO", "user" + i, false);
			logStorageService.ingestLog(log).subscribe();
		}

		// Should handle gracefully without throwing exceptions
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertTrue(logStorageService.getTotalLogs() > 0);
	}

	private LogEntry createSampleLog(String category, String severity, String username, boolean blacklisted) {
		LogEntry log = new LogEntry();
		log.setTimestamp("2025-01-16T12:00:00Z");
		log.setEventCategory(category);
		log.setEventSourceType("linux");
		log.setSeverity(severity);
		log.setUsername(username);
		log.setHostname("testhost");
		log.setRawMessage("test message");
		log.setIsBlacklisted(blacklisted);
		return log;
	}
}