package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dto.StatementDTO;

public class InvalidStatementIdException extends Exception {
    public InvalidStatementIdException(StatementDTO statement) {
        super("Cannot find statement with id " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
    public InvalidStatementIdException(StatementId statementId) {
        super("Cannot find statement with id " + statementId.toString());
    }
}
