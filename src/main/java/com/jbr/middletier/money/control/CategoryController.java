package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.exceptions.CreateCategoryException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.CategoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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

        return categoryManager.getAll();
    }

    @GetMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO>  getIntCategories() {
        LOG.info("Request Categories.");
        return this.getExtCategories();
    }

    @PostMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> createCategory(@RequestBody CategoryDTO category) throws CreateCategoryException {
        LOG.info("Create a new account - {}", category.getId());

        return categoryManager.create(category);
    }

    @PutMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> updateCategory(@RequestBody CategoryDTO category) throws UpdateDeleteCategoryException {
        LOG.info("Update an account - {}", category.getId());

        return categoryManager.update(category);
    }

    @DeleteMapping(path="/int/money/categories")
    public @ResponseBody List<CategoryDTO> deleteCategory(@RequestBody CategoryDTO category) throws UpdateDeleteCategoryException {
        LOG.info("Delete account {}", category.getId());

        return categoryManager.delete(category);
    }
}
