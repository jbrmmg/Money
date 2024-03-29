package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by jason on 07/03/17.
 */
public interface StatementRepository  extends CrudRepository<Statement, StatementId>, JpaSpecificationExecutor<Statement> {
    List<Statement> findByIdAccountAndLocked(Account account, @SuppressWarnings("SameParameterValue") boolean locked);

    List<Statement> findAllByOrderByIdAccountAsc();

    List<Statement> findByIdYear(Integer year);
}
