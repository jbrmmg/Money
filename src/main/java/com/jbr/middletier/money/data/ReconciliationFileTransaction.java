package com.jbr.middletier.money.data;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name="rec_file_tran")
public class ReconciliationFileTransaction {
    @EmbeddedId
    private ReconciliationFileTransactionId id;

    @Column(name="date")
    private LocalDate date;

    @Column(name="amount")
    private Double amount;

    @Column(name="description")
    private String description;

    @Column(name="error")
    private String error;

    public ReconciliationFileTransactionId getId() {
        return id;
    }

    public void setId(ReconciliationFileTransactionId id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
