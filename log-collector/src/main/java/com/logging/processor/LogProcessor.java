package com.logging.processor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logging.forwarder.LogForwarder;
import com.logging.model.ParsedLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogProcessor {

	private final LogForwarder logForwarder;

	private final ObjectMapper objectMapper = new ObjectMapper();;

	// Blacklist for demonstration
	private final Set<String> blacklistedUsers = Set.of("root", "admin", "hacker");
	private final Set<String> blacklistedIPs = Set.of("192.168.1.100", "10.0.0.50");

	// Metrics
	private final AtomicLong totalLogsProcessed = new AtomicLong(0);
	private final Map<String, AtomicLong> categoryMetrics = new ConcurrentHashMap<>();

	// Regex patterns for parsing
	private static final Pattern PRIORITY_PATTERN = Pattern.compile("^<(\\d+)>");
	private static final Pattern LINUX_LOGIN_PATTERN = Pattern.compile("session opened for user (\\w+).*by (\\w+)");
	private static final Pattern LINUX_LOGOUT_PATTERN = Pattern.compile("session closed for user (\\w+)");
	private static final Pattern WINDOWS_LOGIN_PATTERN = Pattern.compile("Account Name: (\\w+)");

	public void processLog(String rawMessage) {
		try {
			// Parse JSON wrapper
			Map<String, String> wrapper = objectMapper.readValue(rawMessage, Map.class);
			String message = wrapper.get("message");

			if (message == null || message.isEmpty()) {
				return;
			}

			// Parse and enrich
			ParsedLog parsedLog = parseLog(message);

			// Forward to central server
			logForwarder.forward(parsedLog);

			// Update metrics
			totalLogsProcessed.incrementAndGet();
			categoryMetrics.computeIfAbsent(parsedLog.getEventCategory(), k -> new AtomicLong(0)).incrementAndGet();

		} catch (Exception e) {
			log.error("Failed to process log: {}", e.getMessage());
		}
	}

	private ParsedLog parseLog(String message) {
		ParsedLog log = new ParsedLog();
		log.setTimestamp(Instant.now().toString());
		log.setRawMessage(message);

		// Extract priority/severity
		Matcher priorityMatcher = PRIORITY_PATTERN.matcher(message);
		if (priorityMatcher.find()) {
			int priority = Integer.parseInt(priorityMatcher.group(1));
			log.setSeverity(getSeverityFromPriority(priority));
		} else {
			log.setSeverity("INFO");
		}

		// Determine category and extract fields
		if (message.contains("sudo") || message.contains("session opened")) {
			log.setEventCategory("linux_login");
			log.setEventSourceType("linux");
			parseLinuxLogin(message, log);
		} else if (message.contains("session closed")) {
			log.setEventCategory("linux_logout");
			log.setEventSourceType("linux");
			parseLinuxLogout(message, log);
		} else if (message.contains("Microsoft-Windows-Security-Auditing")) {
			if (message.contains("logged on")) {
				log.setEventCategory("windows_login");
			} else {
				log.setEventCategory("windows_event");
			}
			log.setEventSourceType("windows");
			parseWindowsLog(message, log);
		} else {
			log.setEventCategory("unknown");
			log.setEventSourceType("unknown");
		}

		// Extract hostname
		String[] parts = message.split("\\s+");
		if (parts.length > 1) {
			log.setHostname(parts[1]);
		}

		// Check blacklist
		boolean isBlacklisted = false;
		if (log.getUsername() != null && blacklistedUsers.contains(log.getUsername())) {
			isBlacklisted = true;
		}
		log.setIsBlacklisted(isBlacklisted);

		return log;
	}

	private void parseLinuxLogin(String message, ParsedLog log) {
		Matcher matcher = LINUX_LOGIN_PATTERN.matcher(message);
		if (matcher.find()) {
			log.setUsername(matcher.group(1));
		}
	}

	private void parseLinuxLogout(String message, ParsedLog log) {
		Matcher matcher = LINUX_LOGOUT_PATTERN.matcher(message);
		if (matcher.find()) {
			log.setUsername(matcher.group(1));
		}
	}

	private void parseWindowsLog(String message, ParsedLog log) {
		Matcher matcher = WINDOWS_LOGIN_PATTERN.matcher(message);
		if (matcher.find()) {
			log.setUsername(matcher.group(1));
		}
	}

	private String getSeverityFromPriority(int priority) {
		int severity = priority & 0x07;
		switch (severity) {
		case 0:
		case 1:
		case 2:
			return "ERROR";
		case 3:
			return "WARN";
		case 4:
		case 5:
			return "INFO";
		default:
			return "DEBUG";
		}
	}

	public Map<String, Object> getMetrics() {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("totalLogsProcessed", totalLogsProcessed.get());

		Map<String, Long> categoryMap = new HashMap<>();
		categoryMetrics.forEach((k, v) -> categoryMap.put(k, v.get()));
		metrics.put("logsByCategory", categoryMap);

		return metrics;
	}
}