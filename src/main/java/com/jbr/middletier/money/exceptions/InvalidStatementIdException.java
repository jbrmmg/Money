package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Statement;

public class InvalidStatementIdException extends Exception {
    public InvalidStatementIdException(Statement statement) {
        super("Cannot find statement with id " + statement.getId().toString());
    }
}
