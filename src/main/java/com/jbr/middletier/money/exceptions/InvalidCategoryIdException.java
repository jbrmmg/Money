package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.CategoryDTO;

public class InvalidCategoryIdException extends Exception {
    public InvalidCategoryIdException(CategoryDTO category) {
        super("Cannot find category with id " + category.getId());
    }
    public InvalidCategoryIdException(String categoryId) {
        super("Cannot find category with id " + categoryId);
    }
}
