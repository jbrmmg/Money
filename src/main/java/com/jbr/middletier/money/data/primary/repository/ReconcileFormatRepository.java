package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.ReconcileFormat;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReconcileFormatRepository extends CrudRepository<ReconcileFormat,String> {
    List<ReconcileFormat> findAllByHeaderLine(String headerLine);

    List<ReconcileFormat> findByHeaderLineIsNull();
}
