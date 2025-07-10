package com.example.pubsubclient;

@FunctionalInterface
public interface ConsumerErrorHandler {
    void onError(Exception e);
}
