package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.StatementDTO;

public class CannotDeleteLockedStatement extends Exception {
    public CannotDeleteLockedStatement(StatementDTO statement) {
        super("Cannot delete locked statement " + statement.getId());
    }
}
