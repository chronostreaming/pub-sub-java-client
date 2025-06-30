package com.example.pubsubclient.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
        UUID id,
        @JsonProperty("data") Object data,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssX") Instant createdAt
) {}
