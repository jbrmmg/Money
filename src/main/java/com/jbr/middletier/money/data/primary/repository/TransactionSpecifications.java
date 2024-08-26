package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Transaction;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class TransactionSpecifications {
    private static final String STATEMENT = "statement";
    private static final String ACCOUNT = "account";
    private static final String ID = "id";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String LOCKED = "locked";

    private TransactionSpecifications() {
        // Prevent implicit public constructor
    }

    public static Specification<Transaction> accountIs(Account account) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get(ACCOUNT), account);
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
