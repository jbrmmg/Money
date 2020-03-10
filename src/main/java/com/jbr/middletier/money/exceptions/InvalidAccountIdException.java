package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Account;

public class InvalidAccountIdException extends Exception {
    public InvalidAccountIdException(Account account) {
        super("Cannot find account with id " + account.getId());
    }
}
