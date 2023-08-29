package com.jbr.middletier.money.exceptions;

public class UpdateDeleteCategoryException extends Exception {
    public UpdateDeleteCategoryException(String categoryId) {
        super("Cannot find category with id " + categoryId);
    }
    public UpdateDeleteCategoryException(String categoryId, String message) {
        super(message + " " + categoryId);
    }
}
