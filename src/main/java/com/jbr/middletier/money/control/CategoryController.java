package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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

    @RequestMapping(path="/ext/money/categories", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Category>  getCategories() {
        LOG.info("Request Categories.");
        return categoryRepository.findAllByOrderBySortAsc();
    }

    @RequestMapping(path="/int/money/categories", method= RequestMethod.GET)
    public @ResponseBody
    Iterable<Category>  getCategories2() {
        LOG.info("Request Categories.");
        return categoryRepository.findAllByOrderBySortAsc();
    }
}
