package com.logging.scheduler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.logging.generator.LogGenerator;
import com.logging.sender.LogSender;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogGeneratorScheduler {

	private final LogSender logSender;

	/**
	 * Spring automatically injects multiple implementations of {@link LogGenerator}
	 */
	private final List<LogGenerator> logGenerators;

	private final ThreadPoolTaskScheduler taskScheduler;

	private ScheduledFuture<?> scheduledTask;

	@PostConstruct
	public void scheduleLogGeneration() {
		log.info("=== Log Client Started ===");
		log.info("Generating logs every 1-2 seconds...");
		log.info("Active generators: " + logGenerators.size());

		scheduledTask = taskScheduler.scheduleWithFixedDelay(this::generateAndSendLogs, Duration.ofSeconds(1));
	}

	private void generateAndSendLogs() {
		for (LogGenerator generator : logGenerators) {
			String logMessage = generator.generateLog();
			log.info("generated log: {}", logMessage);
			logSender.sendLog(logMessage);

			// Random delay between 1-2 seconds
			try {
				Thread.sleep(1000 + (long) (Math.random() * 1000));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@PreDestroy
	public void shutdown() {
		log.info("Shutting down client...");
		if (scheduledTask != null) {
			scheduledTask.cancel(false);
		}
		logSender.close();
	}

}
