package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dto.CategoryDTO;

public class CannotDeleteLockedStatement extends Exception {
    public CannotDeleteLockedStatement(Statement statement) {
        super("Cannot delete locked statement " + statement.getId());
    }
}
