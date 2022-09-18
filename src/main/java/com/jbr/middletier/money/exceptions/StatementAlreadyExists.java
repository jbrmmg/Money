package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;

public class StatementAlreadyExists extends Exception {
    public StatementAlreadyExists(StatementDTO statement) {
        super("Statement already exists - " + statement.getId().toString());
    }
}
