package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.exceptions.CategoryAlreadyExistsException;
import com.jbr.middletier.money.exceptions.DeleteSystemCategoryException;
import com.jbr.middletier.money.exceptions.InvalidCategoryIdException;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 08/02/17.
 */
@Controller
@RequestMapping("/jbr")
public class CategoryController {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public CategoryController(CategoryRepository categoryRepository, ModelMapper modelMapper) {
        this.categoryRepository = categoryRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path="/ext/money/categories")
    public @ResponseBody List<CategoryDTO>  getExtCategories() {
        LOG.info("Request Categories.");

        List<CategoryDTO> result = new ArrayList<>();
        for(Category nextCategory: categoryRepository.findAllByOrderBySortAsc()) {
            result.add(this.modelMapper.map(nextCategory, CategoryDTO.class));
        }

        return result;
    }

    @GetMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO>  getIntCategories() {
        LOG.info("Request Categories.");
        return this.getExtCategories();
    }

    @PostMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> createCategory(@RequestBody CategoryDTO category) throws Exception {
        LOG.info("Create a new account - {}", category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            throw new CategoryAlreadyExistsException(category);
        }

        categoryRepository.save(this.modelMapper.map(category,Category.class));

        return this.getExtCategories();
    }

    @PutMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> updateCategory(@RequestBody CategoryDTO category) throws Exception {
        LOG.info("Update an account - {}", category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(Boolean.TRUE.equals(existingCategory.get().getSystemUse())) {
                throw new Exception(category.getId() + " cannot update system use category.");
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

        return this.getExtCategories();
    }

    @DeleteMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> deleteCategory(@RequestBody CategoryDTO category) throws InvalidCategoryIdException, DeleteSystemCategoryException {
        LOG.info("Delete account {}", category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(Boolean.TRUE.equals(existingCategory.get().getSystemUse())) {
                throw new DeleteSystemCategoryException(category);
            }

            categoryRepository.delete(existingCategory.get());
            return this.getExtCategories();
        }

        throw new InvalidCategoryIdException(category);
    }
}
