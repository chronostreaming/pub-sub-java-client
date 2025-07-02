# Pub/Sub Java Client

This project provides a lightweight Java client for interacting with the [pub-sub-service](#) using its HTTP API. The client is built with the standard `java.net.http.HttpClient` and requires **Java 17** or newer.

## Building

Execute the following command to compile the project and run the tests:

```bash
./gradlew build
```

The compiled JAR will be created under `build/libs/pub-sub-java-client.jar`.

## Getting Started

Add the JAR to your application's classpath and create a `PubSubClient` pointing to the service:

```java
String baseUrl = "http://localhost:8080"; // URL of the pub-sub-service
PubSubClient client = new PubSubClient(baseUrl);
```

## Publishing Events

`EventPublisher` simplifies sending events and notifies an `ErrorHandler` when publishing fails.

```java
record Message(String message) {}

EventPublisherConfig pubCfg = new EventPublisherConfig("my-org", "orders", "processor");
ErrorHandler pubErrHandler = e -> e.printStackTrace();
EventPublisher<Message> publisher = new EventPublisher<>(pubCfg, client, pubErrHandler);

publisher.publish(new EventPublishRequest<>(new Message("alpha")));
```

## Consuming Events

`EventConsumer` polls the service periodically, delegating received events to an `EventsHandler`. Attach an `ErrorHandler` to react to polling failures.

```java
EventsHandler handler = (events, commit) -> {
    // process events then commit
    List<UUID> ids = events.stream().map(EventResponse::id).toList();
    commit.apply(ids);
};

EventConsumerConfig cfg = new EventConsumerConfig(
        "my-org",
        "orders",
        "processor",
        100,
        1000L, // poll every second
        handler);

ErrorHandler errHandler = e -> e.printStackTrace();

EventConsumer consumer = new EventConsumer(client, handler, cfg, errHandler);
consumer.start();

// later when finished
consumer.close();
```

## Running a Worker

For worker-style applications you may want the consumer to keep polling until the
JVM shuts down. A `CountDownLatch` can be used to block the main thread and
ensure the consumer is closed gracefully:

```java
CountDownLatch latch = new CountDownLatch(1);
EventConsumer consumer = new EventConsumer(client, handler, cfg, errHandler);

Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    consumer.close();
    latch.countDown();
}));

consumer.start();
latch.await();
```

