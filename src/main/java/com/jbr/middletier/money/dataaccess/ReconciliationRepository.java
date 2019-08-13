package com.jbr.middletier.money.dataaccess;

import com.jbr.middletier.money.data.ReconciliationData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by jason on 11/04/17.
 */
@Repository
public interface ReconciliationRepository extends CrudRepository<ReconciliationData, Integer> {
    List<ReconciliationData> findAllByOrderByDateAsc();
}
