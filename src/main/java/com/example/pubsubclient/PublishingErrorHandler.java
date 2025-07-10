package com.example.pubsubclient;

import java.util.List;

import com.example.pubsubclient.model.EventPublishRequest;

@FunctionalInterface
public interface PublishingErrorHandler<T> {
    void onError(Exception e, List<EventPublishRequest<T>> eventRequests);
}
