package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.ReconcileFormat;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReconcileFormatRepository extends CrudRepository<ReconcileFormat,String> {
    List<ReconcileFormat> findAllByHeaderLine(String headerLine);
}
