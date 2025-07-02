package com.example.pubsubclient.exception;

public class EventPublishingException extends RuntimeException {
    
    public EventPublishingException(Exception e) {
        super(e);
    }

    public EventPublishingException(String message) {
        super(message);
    }

}

