package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.CategoryDTO;

public class CannotUpdateSystemCategory extends Exception {
    public CannotUpdateSystemCategory(CategoryDTO category) {
        super("Cannot update system category " + category.getId());
    }
}
