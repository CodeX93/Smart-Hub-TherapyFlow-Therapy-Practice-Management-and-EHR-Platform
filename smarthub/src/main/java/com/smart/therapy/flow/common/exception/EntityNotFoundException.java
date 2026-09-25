package com.smart.therapy.flow.common.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, String id) {
        super(String.format("%s with id %s not found", entity, id));
    }

    public EntityNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

