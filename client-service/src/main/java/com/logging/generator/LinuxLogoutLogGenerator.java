package com.logging.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LinuxLogoutLogGenerator implements LogGenerator {

	private final Random random = new Random();
	private final ObjectMapper mapper = new ObjectMapper();

	private final String[] hostnames = { "aiops9242", "server01", "web-server" };
	private final String[] users = { "motadata", "admin", "root", "developer" };

	@Override
	public String generateLog() {
		try {
			String hostname = hostnames[random.nextInt(hostnames.length)];
			String user = users[random.nextInt(users.length)];

			String message = String.format("<86> %s systemd-logind: session closed for user %s", hostname, user);

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
