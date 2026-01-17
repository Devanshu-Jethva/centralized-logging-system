package com.logging.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfig {

	/**
	 * ThreadPoolTaskScheduler is used instead of @Scheduled because:
	 *
	 * - It allows multiple scheduled tasks to run in parallel using a thread pool <br/>
	 * - Prevents blocking when log generation or sending is slow <br/>
	 * - Provides fine-grained control over pool size, thread naming, and shutdown behavior <br/>
	 * - Enables graceful shutdown by waiting for active tasks to complete <br/>
	 * - Is fully managed by Spring <br/>
	 *
	 * @return
	 */
	@Bean
	ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5);
		scheduler.setThreadNamePrefix("log-scheduler-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(30);
		scheduler.initialize(); // Required when manually creating it
		return scheduler;
	}
}
