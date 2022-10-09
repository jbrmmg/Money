package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.DateRange;
import com.jbr.middletier.money.data.Transaction;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.Calendar;

public class TransactionSpecifications {
    private TransactionSpecifications() {
        // Prevent implicit public constructor
    }

    public static Specification<Transaction> accountIn(Iterable<Account> account) {
        // Account id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> accountList = root.get("account");
            return accountList.in(account);
        };
    }

    public static Specification<Transaction> categoryIn(Iterable<Category> category) {
        // Category id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> categoryList = root.get("category");
            return categoryList.in(category);
        };
    }

    public static Specification<Transaction> accountIs(Account account) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("account"), account);
    }

    public static Specification<Transaction> statementDate(LocalDate statementDate) {
        // Get the statement id
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("statement").get("id").get("year"),statementDate.getYear()),
                criteriaBuilder.equal(root.get("statement").get("id").get("month"),statementDate.getMonth()) );
    }

    public static Specification<Transaction> statementIsNull() {
        // Statement is null
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                                                            criteriaBuilder.isNull(root.get("statement").get("id").get("year")),
                                                            criteriaBuilder.isNull(root.get("statement").get("id").get("month")) );
    }

    public static Specification<Transaction> datesBetween(DateRange dateRange) {
        // Strings are dates - from to
        return (root, criteriaQuery, criteriaBuilder) -> {

            Path<LocalDate> dateEntryPath = root.get("date");
            return criteriaBuilder.between(dateEntryPath,dateRange.getFrom(),dateRange.getTo());
        };
    }

    public static Specification<Transaction> notLocked() {
        // locked is true (Y)
        return (root, criteriaQuery, criteriaBuilder) -> {
            root.join("statement", JoinType.LEFT);
            Predicate noStatement = criteriaBuilder.and(
                    criteriaBuilder.isNull(root.get("statement").get("id").get("year")),
                    criteriaBuilder.isNull(root.get("statement").get("id").get("month")) );
            Predicate notLocked = criteriaBuilder.equal(root.get("statement").get("locked"),false);
            return criteriaBuilder.or(noStatement,notLocked);
        };
    }
}
