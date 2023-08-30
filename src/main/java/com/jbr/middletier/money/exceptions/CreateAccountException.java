package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class CreateAccountException extends MoneyException {
    public CreateAccountException(String id) {
        super(HttpStatus.CONFLICT,"Account already exists " + id);
    }
}
