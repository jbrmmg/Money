package com.jbr.middletier.money.exceptions;

public class CreateAccountException extends Exception {
    public CreateAccountException(String id) {
        super("Account already exists " + id);
    }
}
