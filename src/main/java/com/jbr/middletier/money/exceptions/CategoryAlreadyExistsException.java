package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.CategoryDTO;

public class CategoryAlreadyExistsException extends Exception {
    public CategoryAlreadyExistsException(CategoryDTO category) {
        super("Category already exists " + category.getId());
    }
}
