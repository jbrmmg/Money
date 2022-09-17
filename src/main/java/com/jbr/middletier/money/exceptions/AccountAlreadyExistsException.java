package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.AccountDTO;

public class AccountAlreadyExistsException extends Exception {
    public AccountAlreadyExistsException(AccountDTO account) {
        super("Account already exists " + account.getId());
    }
}
