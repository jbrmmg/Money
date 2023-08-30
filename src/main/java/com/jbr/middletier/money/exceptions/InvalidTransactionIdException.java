package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransactionIdException extends MoneyException {
    public InvalidTransactionIdException(int id) {
        super(HttpStatus.CONFLICT, "Cannot find transaction with id " + id);
    }
}
