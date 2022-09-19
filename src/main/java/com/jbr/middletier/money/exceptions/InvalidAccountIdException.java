package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.AccountDTO;

public class InvalidAccountIdException extends Exception {
    public InvalidAccountIdException(AccountDTO account) {
        super("Cannot find account with id " + account.getId());
    }

    public InvalidAccountIdException(String accountId) {
        super("Cannot find account with id " + accountId);
    }
}
