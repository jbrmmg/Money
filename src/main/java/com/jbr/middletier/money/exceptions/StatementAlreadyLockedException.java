package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.LockStatementRequest;
import com.jbr.middletier.money.dto.StatementDTO;

public class StatementAlreadyLockedException extends Exception {
    public StatementAlreadyLockedException(LockStatementRequest request) {
        super("Statement already locked " + request.toString());
    }
}
