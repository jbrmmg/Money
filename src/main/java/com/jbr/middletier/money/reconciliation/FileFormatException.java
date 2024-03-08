package com.jbr.middletier.money.reconciliation;

public class FileFormatException extends Exception {
    public FileFormatException(int lineNumber, String message) {
        super(message + " on line " + lineNumber);
    }
}
