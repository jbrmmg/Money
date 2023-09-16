package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class EmailGenerationException extends MoneyException {
    public EmailGenerationException(String message, Exception cause) {
        super(HttpStatus.FAILED_DEPENDENCY, message, cause);
    }
}
