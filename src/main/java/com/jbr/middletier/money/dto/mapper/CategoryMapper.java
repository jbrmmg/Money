package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dto.CategoryDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class CategoryMapper extends ModelMapper {
    public CategoryMapper() {
        this.createTypeMap(Category.class, CategoryDTO.class);
        this.createTypeMap(CategoryDTO.class, Category.class);
    }
}
