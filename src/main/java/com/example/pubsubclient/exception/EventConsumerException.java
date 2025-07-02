package com.example.pubsubclient.exception;

public class EventConsumerException extends RuntimeException {
    
    public EventConsumerException(Exception e) {
        super(e);
    }

    public EventConsumerException(String message) {
        super(message);
    }

}
