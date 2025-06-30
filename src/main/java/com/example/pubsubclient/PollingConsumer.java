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
public class PollingConsumer implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final PollingConsumerConfig config;
    private ScheduledFuture<?> future;

    public PollingConsumer(PollingConsumerConfig config) {
        this.config = config;
    }

    /** Start polling if not already running. */
    public synchronized void start() {
        if (future != null && !future.isCancelled()) {
            return;
        }
        future = executor.scheduleAtFixedRate(() -> {
            try {
                config.client().consumeEvents(
                        config.org(),
                        config.topic(),
                        config.subscription(),
                        config.batchSize(),
                        config.handler());
            } catch (Exception e) {
                // Print the error but keep polling
                e.printStackTrace();
            }
        }, 0, config.intervalMillis(), TimeUnit.MILLISECONDS);
    }

    /** Stop polling. */
    public synchronized void stop() {
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
