package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.ReconciliationFile;
import com.jbr.middletier.money.data.ReconciliationFileTransaction;
import com.jbr.middletier.money.data.ReconciliationFileTransactionId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReconciliationFileTransactionRepository extends CrudRepository<ReconciliationFileTransaction, ReconciliationFileTransactionId> {
    void deleteById_File(ReconciliationFile file);
    List<ReconciliationFileTransaction> findById_File(ReconciliationFile file);
}
