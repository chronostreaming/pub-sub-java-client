package com.example.pubsubclient;

import com.example.pubsubclient.model.EventResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PubSubClientTest {
    private HttpServer server;
    private String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    void tearDown() {
        server.stop(0);
    }

    @Test
    void testGetOrganizationId() throws Exception {
        UUID orgId = UUID.randomUUID();
        server.createContext("/orgs/test", exchange -> {
            sendJson(exchange, 200, '"' + orgId.toString() + '"');
        });
        PubSubClient client = new PubSubClient(baseUrl);
        Optional<UUID> result = client.getOrganizationId("test");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(orgId, result.get());
    }

    @Test
    void testConsumeEvents() throws Exception {
        server.createContext("/org/topics/topic/subscriptions/sub/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                List<EventResponse> events = List.of(
                        new EventResponse(UUID.randomUUID(), Map.of("message", "hello"), Instant.now()));
                sendJson(exchange, 200, mapper.writeValueAsString(events));
            } else if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 200, "1");
            }
        });
        PubSubClient client = new PubSubClient(baseUrl);
        client.consumeEvents("org", "topic", "sub", 10, (events, commit) -> {
            Assertions.assertFalse(events.isEmpty());
            int committed = commit.apply(List.of(events.get(0).id()));
            Assertions.assertEquals(1, committed);
        });
    }

    @Test
    void testPollingConsumer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/org/topics/topic/subscriptions/sub2/events", exchange -> {
            if (exchange.getRequestMethod().equals("GET")) {
                calls.incrementAndGet();
                List<EventResponse> events = List.of(
                        new EventResponse(UUID.randomUUID(), Map.of("msg", "x"), Instant.now())
                );
                sendJson(exchange, 200, mapper.writeValueAsString(events));
            } else if (exchange.getRequestMethod().equals("POST")) {
                sendJson(exchange, 200, "1");
            }
        });

        PubSubClient client = new PubSubClient(baseUrl);
        EventsHandler handler = (events, commit) -> commit.apply(List.of(events.get().id()));
        PollingConsumerConfig cfg = new PollingConsumerConfig(
                client,
                "org",
                "topic",
                "sub2",
                1,
                100L,
                handler);
        try (PollingConsumer consumer = new PollingConsumer(cfg)) {
            consumer.start();
            Thread.sleep(250);
        }

        Assertions.assertTrue(calls.get() >= 2);
    }

    private void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        // fully consume the request body to avoid connection reset issues
        exchange.getRequestBody().readAllBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes());
        }
        exchange.close();
    }
}
