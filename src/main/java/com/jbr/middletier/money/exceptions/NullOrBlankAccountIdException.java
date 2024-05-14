package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class NullOrBlankAccountIdException extends MoneyException {
    public NullOrBlankAccountIdException() {
        super(HttpStatus.CONFLICT, "Account ID not specified, reconciliation transactions required.");
    }
}
