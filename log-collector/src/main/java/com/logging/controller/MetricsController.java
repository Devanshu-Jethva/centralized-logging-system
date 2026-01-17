package com.logging.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.logging.processor.LogProcessor;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MetricsController {

	private final LogProcessor logProcessor;

	/**
	 * 
	 * @return
	 */
	@GetMapping("/metrics")
	public Map<String, Object> getMetrics() {
		return logProcessor.getMetrics();
	}

	/**
	 * 
	 * @return
	 */
	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "UP", "service", "log-collector");
	}

}
