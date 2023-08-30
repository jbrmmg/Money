package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.manager.CategoryManager;
import org.modelmapper.AbstractConverter;

public class StringCategoryConverter extends AbstractConverter<String, Category> {
    private final CategoryManager categoryManager;

    public StringCategoryConverter(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }
    @Override
    protected Category convert(String s) {
        return categoryManager.getIfValid(s).orElse(null);
    }
}
