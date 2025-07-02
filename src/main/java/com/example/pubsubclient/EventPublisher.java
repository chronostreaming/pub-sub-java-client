package com.example.pubsubclient;

import java.util.List;

import com.example.pubsubclient.model.EventPublishRequest;

public class EventPublisher<T> {

    private final PubSubClient client;
    private final EventPublisherConfig config;
    private final ErrorHandler errorHandler;
    
    public EventPublisher(EventPublisherConfig config, PubSubClient client, ErrorHandler errorHandler) {
        this.config = config;
        this.client = client;
        this.errorHandler = errorHandler;
    }

    public int publish(EventPublishRequest<T> eventRequest) {
        return this.publish(List.of(eventRequest));
    }

    public int publish(List<EventPublishRequest<T>> eventRequests) {
        try {
            return this.client.publishEvents(config.org(), config.topic(), eventRequests);
        } catch(Exception e) {
            errorHandler.onError(e);
            e.printStackTrace();
            return 0;
        }
    }
}
