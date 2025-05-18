package com.jbr.middletier.money.data.primary;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@Entity
@Table(name="rec_file_tran")
public class ReconciliationFileTransaction {
    @EmbeddedId
    private ReconciliationFileTransactionId id;

    @Column(name="date")
    private LocalDate date;

    @Column(name="amount")
    private BigDecimal amount;

    @Column(name="description")
    private String description;

    @Column(name="error")
    private String error;
}
