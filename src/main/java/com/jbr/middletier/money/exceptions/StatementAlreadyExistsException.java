package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;
import org.springframework.http.HttpStatus;

public class StatementAlreadyExistsException extends MoneyException {
    public StatementAlreadyExistsException(StatementDTO statement) {
        super(HttpStatus.CONFLICT, "Statement already exists - " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
}
