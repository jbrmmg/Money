package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.data.Category;

public class DeleteSystemCategoryException extends Exception {
    public DeleteSystemCategoryException(Category category) {
        super("You cannot delete this category as it is used by system. " + category.getId());
    }
}
