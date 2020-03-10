package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Statement;

public class InvalidTransactionIdException extends Exception {
    public InvalidTransactionIdException(int id) {
        super("Cannot find transaction with id " + id);
    }
}
