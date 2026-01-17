package com.logging.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.logging.entity.LogEntry;
import com.logging.service.LogStorageService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class LogController {

	private final LogStorageService logStorageService;

	/**
	 * Ingest logs
	 * 
	 * @param logEntry
	 * @return
	 */
	@PostMapping("/ingest")
	public Mono<ResponseEntity<Map<String, String>>> ingestLog(@RequestBody LogEntry logEntry) {
		return logStorageService.ingestLog(logEntry).then(Mono.just(
				ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success", "message", "Log ingested"))))
				.onErrorResume(e -> {
					if (e.getMessage().contains("overflow")) {
						return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
								.body(Map.of("status", "error", "message", "Backpressure: buffer full")));
					}
					return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(Map.of("status", "error", "message", e.getMessage())));
				});
	}

	/**
	 * Query logs with filters
	 * 
	 * @param service
	 * @param level
	 * @param username
	 * @param isBlacklisted
	 * @param limit
	 * @param sort
	 * @return
	 */
	@GetMapping("/logs")
	public Flux<LogEntry> queryLogs(@RequestParam(required = false) String service,
			@RequestParam(required = false) String level, @RequestParam(required = false) String username,
			@RequestParam(name = "is.blacklisted", required = false) Boolean isBlacklisted,
			@RequestParam(required = false) Integer limit, @RequestParam(required = false) String sort) {

		return logStorageService.queryLogs(service, level, username, isBlacklisted, limit, sort);
	}

	/**
	 * Get system metrics
	 * 
	 * @return
	 */
	@GetMapping("/metrics")
	public Mono<Map<String, Object>> getMetrics() {
		return logStorageService.getMetrics();
	}

	/**
	 * Health check
	 * 
	 * @return
	 */
	@GetMapping("/health")
	public Mono<ResponseEntity<Map<String, Object>>> health() {
		return Mono.just(ResponseEntity
				.ok(Map.of("status", "UP", "service", "log-server", "totalLogs", logStorageService.getTotalLogs())));
	}
}
