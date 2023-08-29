package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.mapper.DtoBasicModelMapper;
import com.jbr.middletier.money.exceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class CategoryManager extends AbstractManager<
        Category,
        CategoryDTO,
        String,
        CategoryRepository,
        CreateCategoryException,
        UpdateDeleteCategoryException> {
    @Autowired
    public CategoryManager(CategoryRepository categoryRepository, DtoBasicModelMapper modelMapper) {
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
        if(instance.getSystemUse()) {
            if(update) {
                throw new UpdateDeleteCategoryException(instance.getId(),"Cannot update system category");
            } else {
                throw new UpdateDeleteCategoryException(instance.getId(),"Cannot delete system category");
            }
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
}
