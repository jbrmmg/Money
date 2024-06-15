package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.Category;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by jason on 04/03/17.
 */
@Repository
public interface CategoryRepository  extends CrudRepository<Category, String> {
    List<Category> findAllByOrderBySortAsc();
}
