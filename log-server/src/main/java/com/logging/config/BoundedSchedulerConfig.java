package com.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class BoundedSchedulerConfig {
	@Bean
	Scheduler logProcessingScheduler() {
		// Bounded thread pool for event-driven processing
		return Schedulers.newBoundedElastic(20, // thread cap
				10_000, // queue size for backpressure
				"log-processor", 60, // TTL seconds
				true // daemon threads
		);
	}
}
