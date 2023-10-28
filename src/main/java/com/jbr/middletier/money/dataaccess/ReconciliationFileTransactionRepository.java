package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.ReconciliationFile;
import com.jbr.middletier.money.data.ReconciliationFileTransaction;
import com.jbr.middletier.money.data.ReconciliationFileTransactionId;
import org.springframework.data.repository.CrudRepository;

public interface ReconciliationFileTransactionRepository extends CrudRepository<ReconciliationFileTransaction, ReconciliationFileTransactionId> {
    void deleteById_File(ReconciliationFile file);
}
