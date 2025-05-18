package com.jbr.middletier.money.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
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

}
