package com.logging.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ParsedLog {

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
	private Boolean isBlacklisted = false;

}
