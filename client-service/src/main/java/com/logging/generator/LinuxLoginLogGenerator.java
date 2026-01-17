package com.logging.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class LinuxLoginLogGenerator implements LogGenerator {

	private final Random random = new Random();
	private final ObjectMapper mapper = new ObjectMapper();

	private final String[] hostnames = { "aiops9242", "server01", "web-server", "db-host" };
	private final String[] users = { "motadata", "admin", "root", "developer", "jenkins" };

	@Override
	public String generateLog() {
		try {
			String hostname = hostnames[random.nextInt(hostnames.length)];
			String user = users[random.nextInt(users.length)];
			String targetUser = random.nextBoolean() ? "root" : user;
			int uid = user.equals("root") ? 0 : 1000 + random.nextInt(100);

			String message = String.format(
					"<86> %s sudo: pam_unix(sudo:session): session opened for user %s(uid=%d) by %s(uid=1000)",
					hostname, targetUser, uid, user);

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