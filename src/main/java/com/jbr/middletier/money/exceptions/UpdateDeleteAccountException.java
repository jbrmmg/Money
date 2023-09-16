package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class UpdateDeleteAccountException extends MoneyException {
    public UpdateDeleteAccountException(String accountId) {
        super(HttpStatus.NOT_FOUND, "Cannot find account with id " + accountId);
    }
}
