package com.jbr.middletier.money.data.primary.repository;

import com.jbr.middletier.money.data.primary.ReportStatus;
import com.jbr.middletier.money.data.primary.ReportStatusId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportStatusRepository extends CrudRepository<ReportStatus, ReportStatusId> {

    @Query("SELECT DISTINCT r.id.year FROM ReportStatus r WHERE r.id.type = 'monthly'")
    List<Integer> findDistinctYearsWithMonthlyReports();

    @Query("SELECT COUNT(r) FROM ReportStatus r WHERE r.id.year = :year AND r.id.type = 'monthly' AND r.successful = true")
    long countSuccessfulMonthlyReports(@Param("year") int year);
}
