package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class ApiError {
    private HttpStatus status;
    private final LocalDateTime timestamp;
    private String message;
    private String debugMessage;

    private ApiError() {
        timestamp = LocalDateTime.now();
    }

    public ApiError(HttpStatus status, String message, Throwable ex) {
        this();
        this.status = status;
        this.message = message;
        this.debugMessage = ex.getLocalizedMessage();
    }

    public LocalDateTime getTimestamp() { return this.timestamp; }

    HttpStatus getStatus() { return this.status; }

    public String getMessage() { return this.message; }

    public String getDebugMessage() { return this.debugMessage; }
}
