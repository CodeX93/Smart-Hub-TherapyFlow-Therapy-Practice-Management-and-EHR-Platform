package com.smart.therapy.flow.common.exception;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {
    private final Object conflictDetails;

    public ConflictException(String message) {
        super(message);
        this.conflictDetails = null;
    }

    public ConflictException(String message, Object conflictDetails) {
        super(message);
        this.conflictDetails = conflictDetails;
    }
}

