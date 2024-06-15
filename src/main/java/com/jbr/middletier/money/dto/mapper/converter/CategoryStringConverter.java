package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.primary.Category;
import org.modelmapper.AbstractConverter;

public class CategoryStringConverter extends AbstractConverter<Category,String> {
    @Override
    protected String convert(Category category) {
        return category.getId();
    }
}
