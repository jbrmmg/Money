package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by jason on 08/02/17.
 */
@Controller
@RequestMapping("/jbr")
public class CategoryController {
    final static private Logger LOG = LoggerFactory.getLogger(CategoryController.class);

    private final
    CategoryRepository categoryRepository;

    @Autowired
    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    @RequestMapping(path="/ext/money/categories", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Category>  getExtCategories() {
        LOG.info("Request Categories.");
        return categoryRepository.findAllByOrderBySortAsc();
    }

    @RequestMapping(path="/int/money/categories", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Category>  getIntCategories() {
        LOG.info("Request Categories.");
        return categoryRepository.findAllByOrderBySortAsc();
    }

    @RequestMapping(path="/int/money/categories",method=RequestMethod.POST)
    public @ResponseBody Iterable<Category> createCategory(@RequestBody Category category) throws Exception {
        LOG.info("Create a new account - " + category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            throw new Exception(category.getId() + " already exists");
        }

        categoryRepository.save(category);

        return categoryRepository.findAll();
    }

    @RequestMapping(path="/int/money/categories",method=RequestMethod.PUT)
    public @ResponseBody Iterable<Category> updateCategory(@RequestBody Category category) throws Exception {
        LOG.info("Update an account - " + category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(existingCategory.get().getSystemUse()) {
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
            throw new Exception(category.getId() + " cannot find category.");
        }

        return categoryRepository.findAll();
    }

    @RequestMapping(path="/int/money/categories",method=RequestMethod.DELETE)
    public @ResponseBody
    StatusResponse deleteAccount(@RequestBody Category category) throws Exception {
        LOG.info("Delete account " + category.getId());

        // Is there an account with this ID?
        Optional<Category> existingCategory = categoryRepository.findById(category.getId());
        if(existingCategory.isPresent()) {
            if(existingCategory.get().getSystemUse()) {
                throw new Exception(category.getId() + " cannot delete system use category.");
            }

            categoryRepository.delete(existingCategory.get());
            return new StatusResponse();
        }

        return new StatusResponse("Category does not exist " + category.getId());
    }
}
