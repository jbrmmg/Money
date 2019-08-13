package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.AllTransaction;
import com.jbr.middletier.money.data.Statement;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.Date;

/**
 * Created by jason on 10/03/17.
 */
public class AllTransactionSpecifications {
    public static Specification<AllTransaction> accountIn(String[] account) {
        // Account id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> accountList = root.get("account");
            return accountList.in((Object[]) account);
        };
    }
    public static Specification<AllTransaction> categoryIn(String[] category) {
        // Category id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> categoryList = root.get("category");
            return categoryList.in((Object[]) category);
        };
    }
    public static Specification<AllTransaction> statement(Date date) {
        // Get the statement id
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("statement"), Statement.getIdFromDateString(date));
    }
    public static Specification<AllTransaction> statementIsNull() {
        // Statement is null
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("statement"),"");
    }
    public static Specification<AllTransaction> datesBetween(Date from, Date to) {
        // Strings are dates - from to
        return (root, criteriaQuery, criteriaBuilder) -> {

            Path<Date> dateEntryPath = root.get("date");
            return criteriaBuilder.between(dateEntryPath,from,to);
        };
    }
    public static Specification<AllTransaction> notLocked() {
        // locked is true (Y)
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("locked"),"N");
    }
}
