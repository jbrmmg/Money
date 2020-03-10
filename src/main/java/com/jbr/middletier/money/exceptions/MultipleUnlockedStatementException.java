package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Account;

public class MultipleUnlockedStatementException extends Exception {
    public MultipleUnlockedStatementException(Account account) {
        super("There are multiple unlocked statements on " + account.getId());
    }
}
