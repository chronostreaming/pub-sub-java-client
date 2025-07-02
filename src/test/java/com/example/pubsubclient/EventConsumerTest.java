package com.example.pubsubclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import static com.example.pubsubclient.TestUtils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventConsumerTest {

    private HttpServer server;
    private String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    void testPollingConsumer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger commitCalls = new AtomicInteger();
        server.createContext("/org/topics/topic/subscriptions/sub2/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                calls.incrementAndGet();
                var message = """
                [{
                    "id": "9f320609-0405-44a3-9042-953a353aa40c",
                    "data": {
                        "message": "aaaaa"
                    },
                    "createdAt": "2025-07-01T23:31:05Z"
                }]
                """;

                sendJson(exchange, 200, message);
            }
        });
        server.createContext("/org/topics/topic/subscriptions/sub2/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                commitCalls.incrementAndGet();
                sendJson(exchange, 200, "1");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventsHandler handler = (events, commit) -> commit.apply(List.of(events.get(0).id()));
        EventConsumerConfig cfg = new EventConsumerConfig(
                "org",
                "topic",
                "sub2",
                1,
                400L,
                handler);
        try (EventConsumer consumer = new EventConsumer(client, handler, cfg)) {
            consumer.start();
            Thread.sleep(1000L);
        }

        Assertions.assertTrue(calls.get() >= 2);
        Assertions.assertEquals(calls.get(), commitCalls.get());
    }

    @Test
    void testPollingConsumerOnGetEventsError() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger commitCalls = new AtomicInteger();
        server.createContext("/org/topics/topic/subscriptions/sub2/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                calls.incrementAndGet();
                sendJson(exchange, 500, "ERROR");
            }
        });
        server.createContext("/org/topics/topic/subscriptions/sub2/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                commitCalls.incrementAndGet();
                sendJson(exchange, 200, "1");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventsHandler handler = (events, commit) -> commit.apply(List.of(events.get(0).id()));
        EventConsumerConfig cfg = new EventConsumerConfig(
                "org",
                "topic",
                "sub2",
                1,
                400L,
                handler);
        try (EventConsumer consumer = new EventConsumer(client, handler, cfg)) {
            consumer.start();
            Thread.sleep(1000L);
        }

        Assertions.assertTrue(calls.get() >= 2);
        Assertions.assertTrue(commitCalls.get() == 0);
    }

    @Test
    void testPollingConsumerOnPostEventCommitsError() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger commitCalls = new AtomicInteger();
        server.createContext("/org/topics/topic/subscriptions/sub2/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                calls.incrementAndGet();
                var message = """
                [{
                    "id": "9f320609-0405-44a3-9042-953a353aa40c",
                    "data": {
                        "message": "aaaaa"
                    },
                    "createdAt": "2025-07-01T23:31:05Z"
                }]
                """;

                sendJson(exchange, 200, message);
            }
        });
        server.createContext("/org/topics/topic/subscriptions/sub2/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                commitCalls.incrementAndGet();
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventsHandler handler = (events, commit) -> commit.apply(List.of(events.get(0).id()));
        EventConsumerConfig cfg = new EventConsumerConfig(
                "org",
                "topic",
                "sub2",
                1,
                400L,
                handler);
        try (EventConsumer consumer = new EventConsumer(client, handler, cfg, e -> System.out.println("aaaaaaa" + e.getLocalizedMessage()))) {
            consumer.start();
            Thread.sleep(1000L);
        }

        Assertions.assertTrue(calls.get() >= 2);
        Assertions.assertTrue(commitCalls.get() == calls.get());
    }

    
}
