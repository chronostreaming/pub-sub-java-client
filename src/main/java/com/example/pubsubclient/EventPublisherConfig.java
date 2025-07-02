package com.example.pubsubclient;

/**
 * Configuration for {@link EventPublisher}. Encapsulates all parameters
 * required to start periodic event polling.
 */
public record EventPublisherConfig(
        String org,
        String topic,
        String subscription
) {}
