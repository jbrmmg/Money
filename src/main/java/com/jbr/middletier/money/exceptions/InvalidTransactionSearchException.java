package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransactionSearchException extends MoneyException {
    public InvalidTransactionSearchException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
