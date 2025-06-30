package com.example.pubsubclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper for publishing events. The data payload is a generic type that will
 * be serialized to JSON when sent to the pub-sub-service.
 */
public record EventPublishRequest<T>(@JsonProperty("data") T data) {}
