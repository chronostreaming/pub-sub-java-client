package com.example.pubsubclient.model;

import java.util.UUID;

public record Subscription(UUID id, String name, UUID topicId) {}
