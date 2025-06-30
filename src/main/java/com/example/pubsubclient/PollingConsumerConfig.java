package com.example.pubsubclient;

/**
 * Configuration for {@link PollingConsumer}. Encapsulates all parameters
 * required to start periodic event polling.
 */
public record PollingConsumerConfig(
        PubSubClient client,
        String org,
        String topic,
        String subscription,
        int batchSize,
        long intervalMillis,
        EventsHandler handler
) {}
