package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.exceptions.CannotUpdateSystemCategory;
import com.jbr.middletier.money.exceptions.CategoryAlreadyExistsException;
import com.jbr.middletier.money.exceptions.DeleteSystemCategoryException;
import com.jbr.middletier.money.exceptions.InvalidCategoryIdException;
import com.jbr.middletier.money.manager.CategoryManager;
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

    private final CategoryManager categoryManager;

    @Autowired
    public CategoryController(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @GetMapping(path="/ext/money/categories")
    public @ResponseBody List<CategoryDTO>  getExtCategories() {
        LOG.info("Request Categories.");

        return categoryManager.getCategories();
    }

    @GetMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO>  getIntCategories() {
        LOG.info("Request Categories.");
        return this.getExtCategories();
    }

    @PostMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> createCategory(@RequestBody CategoryDTO category) throws CategoryAlreadyExistsException {
        LOG.info("Create a new account - {}", category.getId());

        return categoryManager.createCategory(category);
    }

    @PutMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> updateCategory(@RequestBody CategoryDTO category) throws InvalidCategoryIdException, CannotUpdateSystemCategory {
        LOG.info("Update an account - {}", category.getId());

        return categoryManager.updateCategory(category);
    }

    @DeleteMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> deleteCategory(@RequestBody CategoryDTO category) throws InvalidCategoryIdException, DeleteSystemCategoryException {
        LOG.info("Delete account {}", category.getId());

        return categoryManager.deleteCategory(category);
    }
}
