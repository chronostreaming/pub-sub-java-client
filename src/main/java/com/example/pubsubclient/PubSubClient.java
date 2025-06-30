package com.example.pubsubclient;

import com.example.pubsubclient.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class PubSubClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public PubSubClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void createOrganization(String name) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("name", name);
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/orgs"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build());
    }

    public Optional<UUID> getOrganizationId(String orgName) throws IOException, InterruptedException {
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(baseUrl + "/orgs/" + orgName)).GET().build());
        if (resp.statusCode() == 404) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(resp.body().replace("\"", "")));
    }

    public void deleteOrganization(String orgName) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/orgs/" + orgName)).DELETE().build());
    }

    public void createTopic(String orgName, String topicName) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("name", topicName);
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build());
    }

    public Topic getTopic(String orgName, String topicName) throws IOException, InterruptedException {
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName)).GET().build());
        if (resp.statusCode() == 404) {
            throw new RuntimeException("Topic or organization not found");
        }
        return mapper.readValue(resp.body(), Topic.class);
    }

    public void deleteTopic(String orgName, String topicName) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName)).DELETE().build());
    }

    public void createSubscription(String orgName, String topicName, String subscriptionName) throws IOException, InterruptedException {
        Map<String, String> body = Map.of("name", subscriptionName);
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName + "/subscriptions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build());
    }

    public Subscription getSubscription(String orgName, String topicName, String subscriptionName) throws IOException, InterruptedException {
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName + "/subscriptions/" + subscriptionName)).GET().build());
        if (resp.statusCode() == 404) {
            throw new RuntimeException("Subscription, topic or organization not found");
        }
        return mapper.readValue(resp.body(), Subscription.class);
    }

    public void deleteSubscription(String orgName, String topicName, String subscriptionName) throws IOException, InterruptedException {
        send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName + "/subscriptions/" + subscriptionName)).DELETE().build());
    }

    public int publishEvents(String orgName, String topicName, List<EventPublishRequest<?>> events) throws IOException, InterruptedException {
        String body = mapper.writeValueAsString(events);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(baseUrl + "/" + orgName + "/topics/" + topicName + "/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
        return Integer.parseInt(resp.body());
    }

    public List<EventResponse> readEvents(String orgName, String topicName, String subscriptionName, int batchSize) throws IOException, InterruptedException {
        String url = String.format("%s/%s/topics/%s/subscriptions/%s/events?batchSize=%d", baseUrl, orgName, topicName, subscriptionName, batchSize);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url)).GET().build());
        if (resp.statusCode() == 204) {
            return List.of();
        }
        if (resp.statusCode() == 404) {
            throw new RuntimeException("Subscription, topic or organization not found");
        }
        return mapper.readValue(resp.body(), new TypeReference<List<EventResponse>>() {});
    }

    public int commitEvents(String orgName, String topicName, String subscriptionName, List<UUID> eventIds) throws IOException, InterruptedException {
        String url = String.format("%s/%s/topics/%s/subscriptions/%s/events", baseUrl, orgName, topicName, subscriptionName);
        String body = mapper.writeValueAsString(eventIds);
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
        return Integer.parseInt(resp.body());
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
                throw new RuntimeException(e);
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
