package com.example.pubsubclient;

import com.example.pubsubclient.exception.EventConsumerException;
import com.example.pubsubclient.exception.EventPublishingException;
import com.example.pubsubclient.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

class PubSubClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            
    public PubSubClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public <T> int publishEvents(String orgName, String topicName, List<EventPublishRequest<T>> events) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(events);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName + "/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());

        switch (resp.statusCode()) {
            case 200:
                return Integer.parseInt(resp.body());
            case 204:
                return 0;
            case 400:
                throw new EventPublishingException("Error on your request:" + resp.body());
            case 404:
                throw new EventPublishingException("Subscription, topic or organization not found");                
            case 500:
                throw new EventPublishingException("Internal Server Error");
            default:
                return 0;
        }
    }

    public List<EventResponse> readEvents(String orgName, String topicName, String subscriptionName, int batchSize) throws IOException, InterruptedException {
        String url = String.format("%s/%s/topics/%s/subscriptions/%s/events?batchSize=%d", baseUrl, orgName, topicName, subscriptionName, batchSize);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url))
            .GET()
            .header("Content-Type", "application/json")
            .build());
        
        switch (resp.statusCode()) {
            case 200:
                return mapper.readValue(resp.body(), new TypeReference<List<EventResponse>>() {});
            case 204:
                return List.of();
            case 404:
                throw new EventConsumerException("Subscription, topic or organization not found");
            case 409:
                throw new EventConsumerException("Conflict while reading events:s");
            case 500:
                throw new EventConsumerException("Internal Server Error");
            default:
                return List.of();
        }
    }

    public int commitEvents(String orgName, String topicName, String subscriptionName, List<UUID> eventIds) throws IOException, InterruptedException {
        String url = String.format("%s/%s/topics/%s/subscriptions/%s/event-commits", baseUrl, orgName, topicName, subscriptionName);
        String body = mapper.writeValueAsString(eventIds);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());

        switch (resp.statusCode()) {
            case 200:
                return Integer.parseInt(resp.body());
            case 204:
                return 0;
            case 400:
                throw new EventConsumerException("Error on your request:" + resp.body());
            case 404:
                throw new EventConsumerException("Subscription, topic or organization not found");            
            case 500:
                throw new EventConsumerException("Internal Server Error");
            default:
                return 0;
        }
    }

    public void consumeEvents(String org, String topic, String sub, int batchSize, EventsHandler handler) throws Exception {
        List<EventResponse> events = readEvents(org, topic, sub, batchSize);

        if (events.isEmpty()) {
            return;
        }
        Function<List<UUID>, Integer> commitFn = ids -> {
            try {
                return commitEvents(org, topic, sub, ids);
            } catch (Exception e) {
                throw new EventConsumerException(e);
            }
        };
        handler.handle(events, commitFn);
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new RuntimeException("Request failed with status code " + resp.statusCode());
        }
        return resp;
    }
}
