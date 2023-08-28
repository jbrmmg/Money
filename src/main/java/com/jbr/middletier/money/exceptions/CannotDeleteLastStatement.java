package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;

public class CannotDeleteLastStatement extends Exception {
    public CannotDeleteLastStatement(StatementDTO statement) {
        super("Cannot delete last statement " + statement.getAccountId() + " " + statement.getMonth() + " " + statement.getYear());
    }
}
