package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.StatementId;
import org.springframework.http.HttpStatus;

public class StatementAlreadyLockedException extends MoneyException {
    public StatementAlreadyLockedException(StatementId statementId) {
        super(HttpStatus.FORBIDDEN, "Statement already locked " + statementId.toString());
    }
}
