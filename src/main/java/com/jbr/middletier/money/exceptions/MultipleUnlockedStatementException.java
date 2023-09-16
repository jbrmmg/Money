package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Account;
import org.springframework.http.HttpStatus;

public class MultipleUnlockedStatementException extends MoneyException {
    public MultipleUnlockedStatementException(Account account) {
        super(HttpStatus.CONFLICT, "There are multiple unlocked statements on " + account.getId());
    }
}
