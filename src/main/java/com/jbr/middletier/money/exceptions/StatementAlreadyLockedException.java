package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.StatementId;

public class StatementAlreadyLockedException extends Exception {
    public StatementAlreadyLockedException(StatementId statementId) {
        super("Statement already locked " + statementId.toString());
    }
}
