package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.ReconciliationFile;
import org.springframework.data.repository.CrudRepository;

public interface ReconciliationFileRepository extends CrudRepository<ReconciliationFile, String> {
}
