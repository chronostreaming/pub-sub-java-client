package com.example.pubsubclient;

@FunctionalInterface
public interface ErrorHandler {
    void onError(Exception e);
}
