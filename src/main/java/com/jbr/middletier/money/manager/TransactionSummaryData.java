package com.jbr.middletier.money.manager;

import java.math.BigDecimal;
import java.time.LocalDate;

class TransactionSummaryData {
    private int transactionCount;
    private BigDecimal debitSum;
    private BigDecimal creditSum;
    private LocalDate earliest;
    private LocalDate latest;

    public TransactionSummaryData() {
        this.transactionCount = 0;
        this.debitSum = BigDecimal.ZERO;
        this.creditSum = BigDecimal.ZERO;
        this.earliest = null;
        this.latest = null;
    }

    public void incrementCount() {
        this.transactionCount++;
    }

    public void incrementDebit(BigDecimal debit) {
        this.debitSum = this.debitSum.add(debit);
    }

    public void incrementCredit(BigDecimal credit) {
        this.creditSum = this.creditSum.add(credit);
    }

    public void updateEarliest(LocalDate date) {
        if (this.earliest == null || date.isBefore(this.earliest)) {
            this.earliest = date;
        }
    }

    public void updateLatest(LocalDate date) {
        if (this.latest == null || date.isAfter(this.latest)) {
            this.latest = date;
        }
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getDebitSum() {
        return debitSum;
    }

    public BigDecimal getCreditSum() {
        return creditSum;
    }

    public LocalDate getEarliest() {
        return earliest;
    }

    public LocalDate getLatest() {
        return latest;
    }
}
