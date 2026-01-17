package com.logging.service.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.logging.entity.LogEntry;
import com.logging.service.LogStorageService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Scheduler;

@Service
public class LogStorageServiceImpl implements LogStorageService {

	// Thread-safe in-memory storage
	private final Queue<LogEntry> logStore = new ConcurrentLinkedQueue<>();

	private final Scheduler logProcessingScheduler;

	// Metrics counters
	private final AtomicLong totalLogsReceived = new AtomicLong(0);
	private final Map<String, AtomicLong> categoryMetrics = new HashMap<>();
	private final Map<String, AtomicLong> severityMetrics = new HashMap<>();

	// Event-driven approach: sink for reactive streams
	private final Sinks.Many<LogEntry> logSink = Sinks.many().multicast().onBackpressureBuffer(1000); // Backpressure
																										// with bounded
																										// buffer

	public LogStorageServiceImpl(Scheduler logProcessingScheduler) {
		this.logProcessingScheduler = logProcessingScheduler;
		initializeSink();
	}

	private void initializeSink() {
		// Subscribe to the sink and process logs asynchronously
		logSink.asFlux().publishOn(logProcessingScheduler).subscribe(this::processAndStore);
	}

	@Override
	public Mono<Void> ingestLog(LogEntry logEntry) {
		return Mono.fromRunnable(() -> {
			// Non-blocking emit with overflow handling
			Sinks.EmitResult result = logSink.tryEmitNext(logEntry);

			if (result.isFailure()) {
				if (result == Sinks.EmitResult.FAIL_OVERFLOW) {
					// Backpressure: queue is full
					throw new RuntimeException("Log buffer overflow - backpressure activated");
				}
			}
		}).subscribeOn(logProcessingScheduler).then();
	}

	private void processAndStore(LogEntry logEntry) {
		// Store the log
		logStore.add(logEntry);

		// Update metrics
		totalLogsReceived.incrementAndGet();

		if (logEntry.getEventCategory() != null) {
			categoryMetrics.computeIfAbsent(logEntry.getEventCategory(), k -> new AtomicLong(0)).incrementAndGet();
		}

		if (logEntry.getSeverity() != null) {
			severityMetrics.computeIfAbsent(logEntry.getSeverity().toLowerCase(), k -> new AtomicLong(0))
					.incrementAndGet();
		}
	}

	@Override
	public Flux<LogEntry> queryLogs(String service, String level, String username, Boolean isBlacklisted, Integer limit,
			String sort) {
		return Flux.fromIterable(logStore).filter(log -> service == null || service.equals(log.getEventCategory()))
				.filter(log -> level == null || level.equalsIgnoreCase(log.getSeverity()))
				.filter(log -> username == null || username.equals(log.getUsername()))
				.filter(log -> isBlacklisted == null || isBlacklisted.equals(log.getIsBlacklisted())).sort((l1, l2) -> {
					if ("timestamp".equals(sort)) {
						return compareTimestamps(l1.getTimestamp(), l2.getTimestamp());
					}
					return 0;
				}).take(limit != null ? limit : Long.MAX_VALUE).subscribeOn(logProcessingScheduler);
	}

	private int compareTimestamps(String t1, String t2) {
		if (t1 == null && t2 == null) {
			return 0;
		}
		if (t1 == null) {
			return 1;
		}
		if (t2 == null) {
			return -1;
		}
		return t1.compareTo(t2);
	}

	@Override
	public Mono<Map<String, Object>> getMetrics() {
		return Mono.fromCallable(() -> {
			Map<String, Object> metrics = new HashMap<>();
			metrics.put("totalLogsReceived", totalLogsReceived.get());

			Map<String, Long> categoryMap = categoryMetrics.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
			metrics.put("logsByCategory", categoryMap);

			Map<String, Long> severityMap = severityMetrics.entrySet().stream()
					.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
			metrics.put("logsBySeverity", severityMap);

			return metrics;
		}).subscribeOn(logProcessingScheduler);
	}

	@Override
	public long getTotalLogs() {
		return totalLogsReceived.get();
	}
}
