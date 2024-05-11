package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReconciliationFileDTO {
    @Pattern(regexp="^[\\da-zA-Z_./\\\\]{1,40}",message="File can only contain letters or digits up to 45 characters.")
    private String filename;
    private AccountDTO account;
    private LocalDateTime lastModified;
    private Long size;
    @Pattern(regexp="^[\\da-zA-Z]{1,100}$",message="Error can only contain letters or digits up to 45 characters.")
    private String error;
    private int transactionCount;
    private double creditSum;
    private double debitSum;
    private LocalDate earliestTransaction;
    private LocalDate latestTransaction;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public double getCreditSum() {
        return creditSum;
    }

    public void setCreditSum(double creditSum) {
        this.creditSum = creditSum;
    }

    public double getDebitSum() {
        return debitSum;
    }

    public void setDebitSum(double debitSum) {
        this.debitSum = debitSum;
    }

    public LocalDate getEarliestTransaction() {
        return earliestTransaction;
    }

    public void setEarliestTransaction(LocalDate earliestTransaction) {
        this.earliestTransaction = earliestTransaction;
    }

    public LocalDate getLatestTransaction() {
        return latestTransaction;
    }

    public void setLatestTransaction(LocalDate latestTransaction) {
        this.latestTransaction = latestTransaction;
    }
}
