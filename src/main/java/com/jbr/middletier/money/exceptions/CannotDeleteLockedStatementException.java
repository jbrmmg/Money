package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;
import org.springframework.http.HttpStatus;

public class CannotDeleteLockedStatementException extends MoneyException {
    public CannotDeleteLockedStatementException(StatementDTO statement) {
        super(HttpStatus.FORBIDDEN, "Cannot delete locked statement " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
}
