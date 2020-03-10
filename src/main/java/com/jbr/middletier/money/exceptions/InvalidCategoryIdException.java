package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Category;

public class InvalidCategoryIdException extends Exception {
    public InvalidCategoryIdException(Category category) {
        super("Cannot find category with id " + category.getId());
    }
    public InvalidCategoryIdException(String categoryId) {
        super("Cannot find category with id " + categoryId);
    }
}
