package com.jbr.middletier.money.exceptions;

public class UpdateDeleteAccountException extends Exception {
    public UpdateDeleteAccountException(String accountId) {
        super("Cannot find account with id " + accountId);
    }
}
