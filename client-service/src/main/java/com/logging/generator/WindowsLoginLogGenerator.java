package com.logging.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WindowsLoginLogGenerator implements LogGenerator {

	private final Random random = new Random();
	private final ObjectMapper mapper = new ObjectMapper();

	private final String[] hostnames = { "WIN-EQ5V3RA5F7H", "WIN-SERVER01", "WIN-DESKTOP" };
	private final String[] users = { "Motadata", "Administrator", "JohnDoe", "JaneSmith" };

	@Override
	public String generateLog() {
		try {
			String hostname = hostnames[random.nextInt(hostnames.length)];
			String user = users[random.nextInt(users.length)];

			String message = String.format(
					"<134> %s Microsoft-Windows-Security-Auditing: A user account was successfully logged on. Account Name: %s",
					hostname, user);

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
