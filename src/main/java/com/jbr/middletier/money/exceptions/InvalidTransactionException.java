package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends MoneyException {
    public InvalidTransactionException(String message) {
        super(HttpStatus.CONFLICT,message);
    }
}
