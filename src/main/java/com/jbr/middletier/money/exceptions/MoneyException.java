package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class MoneyException extends Exception {
    private final HttpStatus status;

    protected MoneyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected MoneyException(HttpStatus status, String message, Exception cause) {
        super(message,cause);
        this.status = status;
    }

    public MoneyException(HttpStatus status, Exception cause) {
        super(cause.getMessage(),cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return this.status;
    }
}
