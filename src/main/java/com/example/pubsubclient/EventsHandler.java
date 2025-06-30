package com.example.pubsubclient;

import com.example.pubsubclient.model.EventResponse;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@FunctionalInterface
public interface EventsHandler {
    void handle(List<EventResponse> events, Function<List<UUID>, Integer> commitFn) throws Exception;
}
