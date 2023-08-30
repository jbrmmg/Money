package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class CreateCategoryException extends MoneyException {
    public CreateCategoryException(String id) {
        super(HttpStatus.CONFLICT, "Category already exists " + id);
    }
}
