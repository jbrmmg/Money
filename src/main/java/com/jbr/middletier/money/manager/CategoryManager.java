package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.mapper.CategoryMapper;
import com.jbr.middletier.money.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import java.util.Comparator;
import java.util.List;

@Controller
public class CategoryManager extends AbstractManager<
        Category,
        CategoryDTO,
        String,
        CategoryRepository,
        CreateCategoryException,
        UpdateDeleteCategoryException> {
    @Autowired
    public CategoryManager(CategoryMapper modelMapper, CategoryRepository categoryRepository) {
        super(CategoryDTO.class,Category.class,modelMapper,categoryRepository);
    }

    @Override
    String getInstanceId(Category instance) {
        return instance.getId();
    }

    @Override
    CreateCategoryException getAddException(String id) {
        return new CreateCategoryException(id);
    }

    @Override
    UpdateDeleteCategoryException getUpdateDeleteException(String id) {
        return new UpdateDeleteCategoryException(id);
    }

    @Override
    void validateUpdateOrDelete(Category instance, boolean update) throws UpdateDeleteCategoryException {
        // Cannot update or delete system categories
        if(Boolean.TRUE.equals(instance.getSystemUse())) {
            throw new UpdateDeleteCategoryException(instance.getId(),"You cannot " + (update ? "update" : "delete") + " this category as it is used by system.", HttpStatus.FORBIDDEN);
        }

    }

    @Override
    void updateInstance(Category instance, Category from) {
        instance.setColour(from.getColour());
        instance.setName(from.getName());
        instance.setGroup(from.getGroup());
        instance.setRestricted(from.getRestricted());
        instance.setSort(from.getSort());
        instance.setExpense(from.getExpense());
    }

    public List<CategoryDTO> getAllBySortOrder() {
        List<CategoryDTO> result = getAll();
        result.sort(Comparator.comparing(CategoryDTO::getSort));
        return result;
    }
}
