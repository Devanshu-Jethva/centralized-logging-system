package com.logging.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class LogEntry {

	private String timestamp;

	@JsonProperty("event.category")
	private String eventCategory;

	@JsonProperty("event.source.type")
	private String eventSourceType;

	private String username;
	private String hostname;
	private String severity;

	@JsonProperty("raw.message")
	private String rawMessage;

	@JsonProperty("is.blacklisted")
	private Boolean isBlacklisted;

	private String receivedAt;

	public LogEntry() {
		this.receivedAt = Instant.now().toString();
	}

}
