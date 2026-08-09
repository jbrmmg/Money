package com.jbr.middletier.money.data.primary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "report_status")
public class ReportStatus {
    @EmbeddedId
    private ReportStatusId id;

    @Column(name = "total_transactions")
    private int totalTransactions;

    @Column(name = "total_categories")
    private int totalCategories;

    @Column(name = "sum_credits")
    private BigDecimal sumCredits;

    @Column(name = "sum_debits")
    private BigDecimal sumDebits;

    @Column(name = "md5_checksum")
    private String md5Checksum;

    @Column(name = "last_checked")
    private LocalDate lastChecked;

    @Column(name = "successful")
    private boolean successful;
}
