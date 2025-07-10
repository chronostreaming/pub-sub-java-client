package com.example.pubsubclient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Polls the PubSub service at a fixed interval and delegates events to the
 * provided {@link EventsHandler}. Users can start and stop the polling as
 * needed.
 */
public class EventConsumer implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final EventConsumerConfig config;
    private final EventsHandler eventsHandler;
    private final ConsumerErrorHandler errorHandler;
    private final PubSubClient client;

    private ScheduledFuture<?> future;

    public EventConsumer(
            PubSubClient client,
            EventsHandler eventsHandler,
            EventConsumerConfig config) {
        this(client, eventsHandler, config, (error) -> {
        });
    }

    public EventConsumer(
            PubSubClient client,
            EventsHandler eventsHandler,
            EventConsumerConfig config,
            ConsumerErrorHandler errorHandler) {
        this.config = config;
        this.client = client;
        this.eventsHandler = eventsHandler;
        this.errorHandler = errorHandler;
    }

    /** Start polling if not already running. */
    public void start() {
        if (future != null && !future.isCancelled()) {
            return;
        }
        future = executor.scheduleAtFixedRate(() -> {
            try {
                this.client.consumeEvents(
                        config.org(),
                        config.topic(),
                        config.subscription(),
                        config.batchSize(),
                        eventsHandler);
            } catch (Exception e) {
                errorHandler.onError(e);
                e.printStackTrace();
            }
        }, 0, config.intervalMillis(), TimeUnit.MILLISECONDS);
    }

    /** Stop polling. */
    public void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    @Override
    public void close() {
        stop();
        executor.shutdownNow();
    }
}
