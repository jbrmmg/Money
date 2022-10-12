package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.exceptions.CannotUpdateSystemCategory;
import com.jbr.middletier.money.exceptions.CategoryAlreadyExistsException;
import com.jbr.middletier.money.exceptions.DeleteSystemCategoryException;
import com.jbr.middletier.money.exceptions.InvalidCategoryIdException;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class CategoryManager {
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    public CategoryManager(CategoryRepository categoryRepository, ModelMapper modelMapper) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
    }

    public List<CategoryDTO> getCategories() {
        List<CategoryDTO> result = new ArrayList<>();
        for(Category nextCategory: categoryRepository.findAllByOrderBySortAsc()) {
            result.add(this.modelMapper.map(nextCategory, CategoryDTO.class));
        }

        return result;
    }

    public List<CategoryDTO> createCategory(CategoryDTO category) throws CategoryAlreadyExistsException {
        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            throw new CategoryAlreadyExistsException(category);
        }

        categoryRepository.save(this.modelMapper.map(category,Category.class));

        return getCategories();
    }

    public List<CategoryDTO> updateCategory(CategoryDTO category) throws CannotUpdateSystemCategory, InvalidCategoryIdException {
        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(Boolean.TRUE.equals(existingCategory.get().getSystemUse())) {
                throw new CannotUpdateSystemCategory(category);
            }
            existingCategory.get().setColour(category.getColour());
            existingCategory.get().setName(category.getName());
            existingCategory.get().setGroup(category.getGroup());
            existingCategory.get().setRestricted(category.getRestricted());
            existingCategory.get().setSort(category.getSort());
            existingCategory.get().setExpense(category.getExpense());

            categoryRepository.save(existingCategory.get());
        } else {
            throw new InvalidCategoryIdException(category);
        }

        return getCategories();
    }

    public List<CategoryDTO> deleteCategory(CategoryDTO category) throws DeleteSystemCategoryException, InvalidCategoryIdException {
        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(Boolean.TRUE.equals(existingCategory.get().getSystemUse())) {
                throw new DeleteSystemCategoryException(category);
            }

            categoryRepository.delete(existingCategory.get());
            return getCategories();
        }

        throw new InvalidCategoryIdException(category);
    }
}
