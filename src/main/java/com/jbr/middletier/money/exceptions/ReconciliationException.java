package com.jbr.middletier.money.exceptions;

public class ReconciliationException extends Exception {
    public ReconciliationException(int transactionId) {
        super("Failed to reconcile transaction " + transactionId);
    }
}
