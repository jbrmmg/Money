package com.jbr.middletier.money.manager;

import java.time.LocalDate;

class TransactionSummaryData {
    private int transactionCount;
    private double debitSum;
    private double creditSum;
    private LocalDate earliest;
    private LocalDate latest;

    public TransactionSummaryData() {
        this.transactionCount = 0;
        this.debitSum = 0.0;
        this.creditSum = 0.0;
        this.earliest = null;
        this.latest = null;
    }

    public void incrementCount() {
        this.transactionCount++;
    }

    public void incrementDebit(double debit) {
        this.debitSum += debit;
    }

    public void incrementCredit(double credit) {
        this.creditSum += credit;
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

    public double getDebitSum() {
        return debitSum;
    }

    public double getCreditSum() {
        return creditSum;
    }

    public LocalDate getEarliest() {
        return earliest;
    }

    public LocalDate getLatest() {
        return latest;
    }
}
