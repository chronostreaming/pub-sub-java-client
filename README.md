# Pub/Sub Java Client

This project provides a lightweight Java client for interacting with the
[pub-sub-service](#) using its HTTP API. The client is built with the standard
`java.net.http.HttpClient` and requires **Java 17** or newer.

## Building

Execute the following command to compile the project and run the tests:

```bash
./gradlew build
```

The compiled JAR will be created under `build/libs/pub-sub-java-client.jar`.

## Using the Client

Include the JAR on your application's classpath or publish it to your package
repository if desired.

### Creating the Client

```java
String baseUrl = "http://localhost:8080"; // URL of the pub-sub-service
PubSubClient client = new PubSubClient(baseUrl);
```

### Organizations and Topics

```java
client.createOrganization("my-org");
Optional<UUID> orgId = client.getOrganizationId("my-org");
client.createTopic("my-org", "orders");
```

### Subscriptions

```java
client.createSubscription("my-org", "orders", "processor");
Subscription sub = client.getSubscription("my-org", "orders", "processor");
```

### Publishing Events

```java
record Message(String message) {}

List<EventPublishRequest<Message>> events = List.of(
    new EventPublishRequest<>(new Message("alpha")),
    new EventPublishRequest<>(new Message("bravo"))
);
client.publishEvents("my-org", "orders", events);
```

`EventPublishRequest<T>` accepts any Java object and the client will
serialize it to JSON before sending it to the service.

### Consuming Events

`consumeEvents` reads the next batch of events and passes them to an
`EventsHandler`. The handler receives the list of events and a commit function
to acknowledge processing.

```java
client.consumeEvents("my-org", "orders", "processor", 100, (events, commit) -> {
    for (EventResponse e : events) {
        System.out.println(e.data());
    }

    // commit once processing is successful
    List<UUID> ids = events.stream().map(EventResponse::id).toList();
    commit.apply(ids);
});
```

### Cleaning Up

```java
client.deleteSubscription("my-org", "orders", "processor");
client.deleteTopic("my-org", "orders");
client.deleteOrganization("my-org");
```

## API Compatibility

The client implements the endpoints described by the service's OpenAPI
specification. A shortened excerpt is shown below:

```
POST /orgs
GET /orgs/{orgName}
DELETE /orgs/{orgName}
POST /{orgName}/topics
GET /{orgName}/topics/{topicName}
DELETE /{orgName}/topics/{topicName}
POST /{orgName}/topics/{topicName}/subscriptions
GET /{orgName}/topics/{topicName}/subscriptions/{subscriptionName}
DELETE /{orgName}/topics/{topicName}/subscriptions/{subscriptionName}
POST /{orgName}/topics/{topicName}/events
GET  /{orgName}/topics/{topicName}/subscriptions/{subscriptionName}/events
POST /{orgName}/topics/{topicName}/subscriptions/{subscriptionName}/events
```

A `RuntimeException` is thrown when the service returns an error status (>=400).
