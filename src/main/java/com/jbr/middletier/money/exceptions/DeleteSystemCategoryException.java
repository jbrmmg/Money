package com.jbr.middletier.money.exceptions;


import com.jbr.middletier.money.dto.CategoryDTO;

public class DeleteSystemCategoryException extends Exception {
    public DeleteSystemCategoryException(CategoryDTO category) {
        super("You cannot delete this category as it is used by system. " + category.getId());
    }
}
