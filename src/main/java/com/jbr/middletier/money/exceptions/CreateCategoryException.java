package com.jbr.middletier.money.exceptions;

public class CreateCategoryException extends Exception {
    public CreateCategoryException(String id) {
        super("Category already exists " + id);
    }
}
