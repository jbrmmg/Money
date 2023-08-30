package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class UpdateDeleteCategoryException extends MoneyException {
    public UpdateDeleteCategoryException(String categoryId) {
        super(HttpStatus.NOT_FOUND, "Cannot find category with id " + categoryId);
    }
    public UpdateDeleteCategoryException(String categoryId, String message, HttpStatus status) {
        super(status, message + " (" + categoryId + ")");
    }
}
