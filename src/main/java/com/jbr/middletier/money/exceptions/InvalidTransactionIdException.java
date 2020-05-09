package com.jbr.middletier.money.exceptions;

public class InvalidTransactionIdException extends Exception {
    public InvalidTransactionIdException(int id) {
        super("Cannot find transaction with id " + id);
    }
}
