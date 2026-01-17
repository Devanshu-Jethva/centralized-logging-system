package com.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class BoundedSchedulerConfig {

	@Bean
	Scheduler logProcessingScheduler() {
		return Schedulers.newBoundedElastic(30, // thread cap
				20000, // queue size
				"collector-processor", 60, true);
	}
}
