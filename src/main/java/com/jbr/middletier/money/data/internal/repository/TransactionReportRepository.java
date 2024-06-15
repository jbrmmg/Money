package com.jbr.middletier.money.data.internal.repository;

import com.jbr.middletier.money.data.internal.TransactionReport;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TransactionReportRepository extends CrudRepository<TransactionReport, Integer>, JpaSpecificationExecutor<TransactionReport> {
    @NotNull
    List<TransactionReport> findAll(@NotNull Specification<TransactionReport> spec, @NotNull Sort sort);

    @NotNull
    List<TransactionReport> findByTransactionId(@NotNull Integer transactionId);
}
