package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;
import org.springframework.http.HttpStatus;

public class CannotDeleteLastStatementException extends MoneyException {
    public CannotDeleteLastStatementException(StatementDTO statement) {
        super(HttpStatus.FORBIDDEN, "Cannot delete last statement " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
}
