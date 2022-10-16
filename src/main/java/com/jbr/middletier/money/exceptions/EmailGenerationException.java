package com.jbr.middletier.money.exceptions;

public class EmailGenerationException extends Exception {
    public EmailGenerationException(String message, Exception cause) {
        super(message,cause);
    }
}
