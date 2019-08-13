package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.AllTransaction;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by jason on 09/03/17.
 */
public interface AllTransactionRepository extends CrudRepository<AllTransaction, String>, JpaSpecificationExecutor {
}
