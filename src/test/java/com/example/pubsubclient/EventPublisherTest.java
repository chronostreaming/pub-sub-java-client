package com.example.pubsubclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.sun.net.httpserver.HttpServer;
import com.example.pubsubclient.model.EventPublishRequest;
import static com.example.pubsubclient.TestUtils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventPublisherTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void beforeEveryTest() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void afterEveryTest() {
        server.stop(1);
    }

    @Test
    void testEventPublishing() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/org/topics/topic/events", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                calls.incrementAndGet();
                sendJson(exchange, 200, "1");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventPublisherConfig cfg = new EventPublisherConfig("org", "topic", "sub");
        AtomicInteger errors = new AtomicInteger();
        PublishingErrorHandler<String> errorHandler = (error, events) -> errors.incrementAndGet();
        EventPublisher<String> publisher = new EventPublisher<>(cfg, client, errorHandler);

        int result = publisher.publish(new EventPublishRequest<>("data"));

        Assertions.assertEquals(1, result);
        Assertions.assertEquals(1, calls.get());
        Assertions.assertEquals(0, errors.get());
    }

    @Test
    void testEventPublishingOnError() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/org/topics/topic/events", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                calls.incrementAndGet();
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventPublisherConfig cfg = new EventPublisherConfig("org", "topic", "sub");
        AtomicInteger errors = new AtomicInteger();
        PublishingErrorHandler<String> errorHandler = (error, events) -> errors.incrementAndGet();
        EventPublisher<String> publisher = new EventPublisher<>(cfg, client, errorHandler);

        int result = publisher.publish(new EventPublishRequest<>("data"));

        Assertions.assertEquals(0, result);
        Assertions.assertEquals(1, calls.get());
        Assertions.assertEquals(1, errors.get());
    }
}
