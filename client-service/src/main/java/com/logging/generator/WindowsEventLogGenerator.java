package com.logging.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WindowsEventLogGenerator implements LogGenerator {

	private final Random random = new Random();
	private final ObjectMapper mapper = new ObjectMapper();

	private final String[] hostnames = { "WIN-EQ5V3RA5F7H", "WIN-SERVER01" };
	private final String[] events = { "Application error occurred", "Service started successfully",
			"System shutdown initiated", "Disk space low warning" };

	@Override
	public String generateLog() {
		try {
			String hostname = hostnames[random.nextInt(hostnames.length)];
			String event = events[random.nextInt(events.length)];

			String message = String.format("<134> %s Microsoft-Windows-EventLog: %s", hostname, event);

			Map<String, String> logWrapper = new HashMap<>();
			logWrapper.put("message", message);

			return mapper.writeValueAsString(logWrapper);
		} catch (Exception e) {
			return """
					{
						"message": "error"
					}
					""";
		}
	}
}