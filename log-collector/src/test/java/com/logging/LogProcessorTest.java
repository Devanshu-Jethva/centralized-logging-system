package com.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.logging.forwarder.LogForwarder;
import com.logging.processor.LogProcessor;

class LogProcessorTest {

	private LogProcessor logProcessor;

	@Mock
	private LogForwarder logForwarder;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		logProcessor = new LogProcessor(logForwarder);
	}

	@Test
	void testProcessLinuxLoginLog() {
		String rawLog = "{\"message\":\"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0) by motadata(uid=1000)\"}";

		logProcessor.processLog(rawLog);

		// Wait for async processing
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		verify(logForwarder, times(1)).forward(any());

		Map<String, Object> metrics = logProcessor.getMetrics();
		assertEquals(1L, metrics.get("totalLogsProcessed"));
	}

	@Test
	void testProcessWindowsLoginLog() {
		String rawLog = "{\"message\":\"<134> WIN-EQ5V3RA5F7H Microsoft-Windows-Security-Auditing: A user account was successfully logged on. Account Name: Motadata\"}";

		logProcessor.processLog(rawLog);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		verify(logForwarder, times(1)).forward(any());

		Map<String, Object> metrics = logProcessor.getMetrics();
		assertEquals(1L, metrics.get("totalLogsProcessed"));
	}

	@Test
	void testBlacklistDetection() {
		String rawLog = "{\"message\":\"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0) by admin(uid=1000)\"}";

		logProcessor.processLog(rawLog);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		verify(logForwarder, times(1)).forward(argThat(log -> log.getIsBlacklisted() == true));
	}

	@Test
	void testSeverityExtraction() {
		String errorLog = "{\"message\":\"<3> server01 kernel: critical error occurred\"}";
		String infoLog = "{\"message\":\"<86> server01 info: normal operation\"}";

		logProcessor.processLog(errorLog);
		logProcessor.processLog(infoLog);

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		verify(logForwarder, times(2)).forward(any());
	}

	@Test
	void testInvalidLogHandling() {
		String invalidLog = "{\"invalid\":\"json\"}";

		// Should not throw exception
		assertDoesNotThrow(() -> logProcessor.processLog(invalidLog));

		// Should not forward invalid logs
		verify(logForwarder, never()).forward(any());
	}

	@Test
	void testMetricsAccuracy() {
		String log1 = "{\"message\":\"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0) by motadata(uid=1000)\"}";
		String log2 = "{\"message\":\"<134> WIN-EQ5V3RA5F7H Microsoft-Windows-Security-Auditing: A user account was successfully logged on. Account Name: Motadata\"}";
		String log3 = "{\"message\":\"<86> aiops9242 sudo: pam_unix(sudo:session): session opened for user root(uid=0) by developer(uid=1000)\"}";

		logProcessor.processLog(log1);
		logProcessor.processLog(log2);
		logProcessor.processLog(log3);

		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Map<String, Object> metrics = logProcessor.getMetrics();
		assertEquals(3L, metrics.get("totalLogsProcessed"));

		@SuppressWarnings("unchecked")
		Map<String, Long> categoryMetrics = (Map<String, Long>) metrics.get("logsByCategory");
		assertEquals(2L, categoryMetrics.get("linux_login"));
		assertEquals(1L, categoryMetrics.get("windows_login"));
	}

	@Test
	void testConcurrentProcessing() {
		// Simulate concurrent log processing
		for (int i = 0; i < 100; i++) {
			String log = "{\"message\":\"<86> server" + i
					+ " sudo: pam_unix(sudo:session): session opened for user user" + i
					+ "(uid=1000) by user(uid=1000)\"}";
			new Thread(() -> logProcessor.processLog(log)).start();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		Map<String, Object> metrics = logProcessor.getMetrics();
		assertEquals(100L, metrics.get("totalLogsProcessed"));
	}
}