package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.ReconciliationFile;
import com.jbr.middletier.money.data.primary.ReconciliationFileTransaction;
import com.jbr.middletier.money.data.primary.ReconciliationFileTransactionId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ReconciliationFileTransactionRepository extends CrudRepository<ReconciliationFileTransaction, ReconciliationFileTransactionId> {
    void deleteById_File(ReconciliationFile file);
    List<ReconciliationFileTransaction> findById_File(ReconciliationFile file);
}
