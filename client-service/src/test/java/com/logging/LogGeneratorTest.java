package com.logging;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logging.generator.LinuxLoginLogGenerator;
import com.logging.generator.LinuxLogoutLogGenerator;
import com.logging.generator.LogGenerator;
import com.logging.generator.WindowsEventLogGenerator;
import com.logging.generator.WindowsLoginLogGenerator;

class LogGeneratorTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void testLinuxLoginLogGeneration() throws Exception {
		LinuxLoginLogGenerator generator = new LinuxLoginLogGenerator();
		String log = generator.generateLog();

		assertNotNull(log);
		assertTrue(log.contains("message"));

		Map<String, String> logMap = mapper.readValue(log, Map.class);
		String message = logMap.get("message");

		assertTrue(message.contains("sudo"));
		assertTrue(message.contains("session opened"));
		assertTrue(message.matches(".*<\\d+>.*")); // Contains priority
	}

	@Test
	void testLinuxLogoutLogGeneration() throws Exception {
		LinuxLogoutLogGenerator generator = new LinuxLogoutLogGenerator();
		String log = generator.generateLog();

		assertNotNull(log);

		Map<String, String> logMap = mapper.readValue(log, Map.class);
		String message = logMap.get("message");

		assertTrue(message.contains("session closed"));
		assertTrue(message.matches(".*<\\d+>.*"));
	}

	@Test
	void testWindowsLoginLogGeneration() throws Exception {
		WindowsLoginLogGenerator generator = new WindowsLoginLogGenerator();
		String log = generator.generateLog();

		assertNotNull(log);

		Map<String, String> logMap = mapper.readValue(log, Map.class);
		String message = logMap.get("message");

		assertTrue(message.contains("Microsoft-Windows-Security-Auditing"));
		assertTrue(message.contains("logged on"));
		assertTrue(message.contains("Account Name"));
	}

	@Test
	void testWindowsEventLogGeneration() throws Exception {
		WindowsEventLogGenerator generator = new WindowsEventLogGenerator();
		String log = generator.generateLog();

		assertNotNull(log);

		Map<String, String> logMap = mapper.readValue(log, Map.class);
		String message = logMap.get("message");

		assertTrue(message.contains("Microsoft-Windows-EventLog"));
		assertTrue(message.matches(".*<\\d+>.*"));
	}

	@Test
	void testLogGeneratorsProduceValidJson() {
		LogGenerator[] generators = { new LinuxLoginLogGenerator(), new LinuxLogoutLogGenerator(),
				new WindowsLoginLogGenerator(), new WindowsEventLogGenerator() };

		for (LogGenerator generator : generators) {
			String log = generator.generateLog();
			assertDoesNotThrow(() -> mapper.readValue(log, Map.class),
					"Generator " + generator.getClass().getSimpleName() + " should produce valid JSON");
		}
	}

	@Test
	void testMultipleGenerations() {
		LinuxLoginLogGenerator generator = new LinuxLoginLogGenerator();

		for (int i = 0; i < 10; i++) {
			String log = generator.generateLog();
			assertNotNull(log);
			assertDoesNotThrow(() -> mapper.readValue(log, Map.class));
		}
	}

	@Test
	void testLogVariety() throws Exception {
		LinuxLoginLogGenerator generator = new LinuxLoginLogGenerator();

		String log1 = generator.generateLog();
		String log2 = generator.generateLog();
		String log3 = generator.generateLog();

		// Logs should have variety (different users/hosts)
		// This is probabilistic, but with 3 generations it's very likely
		assertNotEquals(log1, log2);
	}
}