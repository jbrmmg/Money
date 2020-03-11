package com.jbr.middletier.money.exceptions;

public class EmptyMatchDataException extends Exception{
    public EmptyMatchDataException() {
        super("Empty Match Data - cannot perform auto accept.");
    }
}
