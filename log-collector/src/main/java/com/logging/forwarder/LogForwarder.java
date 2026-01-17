package com.logging.forwarder;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.logging.model.ParsedLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogForwarder {

	private final WebClient webClient;

	public void forward(ParsedLog parsedLog) {
		webClient.post().uri("/ingest").bodyValue(parsedLog).retrieve().bodyToMono(String.class)
				.retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).maxBackoff(Duration.ofSeconds(5)))
				.subscribe(response -> {
					// Success - no action needed
				}, error -> {
					log.error("Failed to forward log after retries: " + error.getMessage());
				});
	}
}