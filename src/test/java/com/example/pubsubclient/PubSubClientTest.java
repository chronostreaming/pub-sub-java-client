package com.example.pubsubclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.sun.net.httpserver.HttpServer;
import com.example.pubsubclient.exception.EventConsumerException;
import static com.example.pubsubclient.TestUtils.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PubSubClientTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(1);
    }

    @Test
    void testPublishEventsSuccess() throws Exception {
        server.createContext("/org/topics/topic/events", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 200, "2");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        int result = client.publishEvents(
                "org",
                "topic",
                List.of(new com.example.pubsubclient.model.EventPublishRequest<>("data")));

        Assertions.assertEquals(2, result);
    }

    @Test
    void testPublishEventsError() throws Exception {
        server.createContext("/org/topics/topic/events", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);

        Assertions.assertThrows(RuntimeException.class, () ->
            client.publishEvents(
                "org",
                "topic",
                List.of(new com.example.pubsubclient.model.EventPublishRequest<>("data"))));
    }

    @Test
    void testReadEventsSuccess() throws Exception {
        String msg = """
            [{
                "id": "%s",
                "data": {"msg": "hello"},
                "createdAt": "2025-07-01T00:00:00Z"
            }]
            """.formatted(UUID.randomUUID());
        server.createContext("/org/topics/topic/subscriptions/sub/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                sendJson(exchange, 200, msg);
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        var events = client.readEvents("org", "topic", "sub", 1);
        Assertions.assertEquals(1, events.size());
    }

    @Test
    void testReadEventsError() throws Exception {
        server.createContext("/org/topics/topic/subscriptions/sub/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);

        Assertions.assertThrows(RuntimeException.class, () ->
            client.readEvents("org", "topic", "sub", 1));
    }

    @Test
    void testCommitEventsSuccess() throws Exception {
        server.createContext("/org/topics/topic/subscriptions/sub/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 200, "1");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        int result = client.commitEvents(
                "org",
                "topic",
                "sub",
                List.of(UUID.randomUUID()));
        Assertions.assertEquals(1, result);
    }

    @Test
    void testCommitEventsError() throws Exception {
        server.createContext("/org/topics/topic/subscriptions/sub/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);

        Assertions.assertThrows(RuntimeException.class, () ->
            client.commitEvents(
                "org",
                "topic",
                "sub",
                List.of(UUID.randomUUID())));
    }

    @Test
    void testConsumeEventsReadError() throws Exception {
        server.createContext("/org/topics/topic/subscriptions/sub/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);

        Assertions.assertThrows(RuntimeException.class, () ->
            client.consumeEvents("org", "topic", "sub", 1, (events, commit) -> {}));
    }

    @Test
    void testConsumeEventsCommitError() throws Exception {
        String msg = """
            [{
                "id": "9f320609-0405-44a3-9042-953a353aa40c",
                "data": {"msg": "hello"},
                "createdAt": "2025-07-01T00:00:00Z"
            }]
            """;
        server.createContext("/org/topics/topic/subscriptions/sub/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                sendJson(exchange, 200, msg);
            }
        });
        server.createContext("/org/topics/topic/subscriptions/sub/event-commits", exchange -> {
            if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 500, "error");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);

        Assertions.assertThrows(EventConsumerException.class, () ->
            client.consumeEvents("org", "topic", "sub", 1, (events, commit) ->
                commit.apply(List.of(events.get(0).id()))));
    }
}
