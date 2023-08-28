package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.data.Transaction;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.List;

public class TransactionSpecifications {
    private static final String STATEMENT = "statement";
    private static final String ACCOUNT = "account";
    private static final String CATEGORY = "category";
    private static final String ID = "id";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DATE = "date";
    private static final String LOCKED = "locked";

    private TransactionSpecifications() {
        // Prevent implicit public constructor
    }

    public static Specification<Transaction> accountIn(List<Account> account) {
        // Account id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> accountList = root.get(ACCOUNT);
            return accountList.in(account);
        };
    }

    public static Specification<Transaction> categoryIn(List<Category> category) {
        // Category id in a list of values.
        return (root, criteriaQuery, criteriaBuilder) -> {
            final Path<String> categoryList = root.get(CATEGORY);
            return categoryList.in(category);
        };
    }

    public static Specification<Transaction> accountIs(Account account) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get(ACCOUNT), account);
    }

    public static Specification<Transaction> statementDate(LocalDate statementDate) {
        // Get the statement id
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get(STATEMENT).get(ID).get(YEAR),statementDate.getYear()),
                criteriaBuilder.equal(root.get(STATEMENT).get(ID).get(MONTH),statementDate.getMonthValue()) );
    }

    public static Specification<Transaction> statementIsNull() {
        // Statement is null
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.and(
                                                            criteriaBuilder.isNull(root.get(STATEMENT).get(ID).get(YEAR)),
                                                            criteriaBuilder.isNull(root.get(STATEMENT).get(ID).get(MONTH)) );
    }

    public static Specification<Transaction> datesBetween(DateRangeDTO dateRange) {
        // Strings are dates - from to
        return (root, criteriaQuery, criteriaBuilder) -> {

            Path<LocalDate> dateEntryPath = root.get(DATE);
            return criteriaBuilder.between(dateEntryPath,dateRange.getFrom(),dateRange.getTo());
        };
    }

    public static Specification<Transaction> notLocked() {
        // locked is true (Y)
        return (root, criteriaQuery, criteriaBuilder) -> {
            Predicate noStatement = criteriaBuilder.and(
                    criteriaBuilder.isNull(root.join(STATEMENT, JoinType.LEFT).get(ID).get(YEAR)),
                    criteriaBuilder.isNull(root.join(STATEMENT, JoinType.LEFT).get(ID).get(MONTH)) );
            Predicate notLocked = criteriaBuilder.equal(root.join(STATEMENT, JoinType.LEFT).get(LOCKED),false);
            return criteriaBuilder.or(noStatement,notLocked);
        };
    }
}
