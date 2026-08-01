package com.jbr.middletier.money.data.internal.repository;

import com.jbr.middletier.money.data.internal.TransactionReport;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionReportRepository extends CrudRepository<TransactionReport, Integer>, JpaSpecificationExecutor<TransactionReport> {
    @NotNull
    List<TransactionReport> findAll(@Nullable Specification<TransactionReport> spec, @NotNull Sort sort);

    @NotNull
    List<TransactionReport> findByTransactionId(@NotNull Integer transactionId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionReport t WHERE t.accountId = :accountId AND t.statementYear = :year AND t.statementMonth = :month AND t.date < :date")
    BigDecimal sumAmountsByAccountAndStatementBeforeDate(
            @Param("accountId") String accountId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("date") String date);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionReport t WHERE t.accountId = :accountId AND t.statementYear IS NULL AND t.statementMonth IS NULL AND t.date < :date")
    BigDecimal sumAmountsByAccountNoStatementBeforeDate(
            @Param("accountId") String accountId,
            @Param("date") String date);
}
