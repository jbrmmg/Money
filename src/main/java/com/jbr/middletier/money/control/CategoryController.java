package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.exceptions.CreateCategoryException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.CategoryManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Categories", description = "File type classification rules used during transaction categorisation")
public class CategoryController {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryManager categoryManager;

    @Autowired
    public CategoryController(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @GetMapping(path="/categories")
    public List<CategoryDTO> getCategories() {
        LOG.info("Request Categories.");
        return categoryManager.getAllBySortOrder();
    }

    @PostMapping(path="/categories")
    public List<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO category) throws CreateCategoryException {
        LOG.info("Create a new category - {}", category.getId());
        return categoryManager.create(category);
    }

    @PutMapping(path="/categories")
    public List<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO category) throws UpdateDeleteCategoryException {
        LOG.info("Update a category - {}", category.getId());
        return categoryManager.update(category);
    }

    @DeleteMapping(path="/categories")
    public List<CategoryDTO> deleteCategory(@Valid @RequestBody CategoryDTO category) throws UpdateDeleteCategoryException {
        LOG.info("Delete category {}", category.getId());
        return categoryManager.delete(category);
    }
}
