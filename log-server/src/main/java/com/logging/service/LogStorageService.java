package com.logging.service;

import java.util.Map;

import com.logging.entity.LogEntry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LogStorageService {

	/**
	 * Ingest log to in memory log store
	 * 
	 * @param logEntry
	 * @return
	 */
	Mono<Void> ingestLog(LogEntry logEntry);

	/**
	 * Query logs with filters
	 * 
	 * Supported filters: - service: event.category (e.g., linux_login, windows_logout) - level: severity (e.g., error,
	 * warn, info) - username: username field - is.blacklisted: boolean
	 * 
	 * Options: - limit: max results - sort: sort field (timestamp)
	 * 
	 * @param service
	 * @param level
	 * @param username
	 * @param isBlacklisted
	 * @param limit
	 * @param sort
	 * @return
	 */
	Flux<LogEntry> queryLogs(String service, String level, String username, Boolean isBlacklisted, Integer limit,
			String sort);

	/**
	 * get metrics like -> totalLogsReceived, logsBySeverity and logsByCategory
	 * 
	 * @return
	 */
	Mono<Map<String, Object>> getMetrics();

	/**
	 * 
	 * @return
	 */
	long getTotalLogs();

}
