package com.sparta.logistics.hub.kafka.exception;

public class KafkaSkipException extends RuntimeException {
    public KafkaSkipException(String message) {
        super(message);
    }
}
