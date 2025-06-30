package com.example.pubsubclient.model;

import java.util.UUID;

public record Topic(UUID id, String name, UUID organizationId) {}
