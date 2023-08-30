package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dto.StatementDTO;
import org.springframework.http.HttpStatus;

public class InvalidStatementIdException extends MoneyException {
    public InvalidStatementIdException(StatementDTO statement) {
        super(HttpStatus.NOT_FOUND, "Cannot find statement with id " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
    public InvalidStatementIdException(StatementId statementId) {
        super(HttpStatus.NOT_FOUND, "Cannot find statement with id " + statementId.toString());
    }
}
