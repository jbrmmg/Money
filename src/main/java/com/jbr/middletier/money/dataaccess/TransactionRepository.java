package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Transaction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by jason on 07/03/17.
 */
public interface TransactionRepository extends CrudRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByAccountAndStatementIdYearAndStatementIdMonth(Account account, Integer statementYear, Integer statementMonth);
}
