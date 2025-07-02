package com.example.pubsubclient;

/**
 * Configuration for {@link EventConsumer}. Encapsulates all parameters
 * required to start periodic event polling.
 */
public record EventConsumerConfig(
        String org,
        String topic,
        String subscription,
        int batchSize,
        long intervalMillis,
        EventsHandler handler
) {}
