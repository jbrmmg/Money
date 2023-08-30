package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;

import java.time.LocalDate;


public class TransactionDTO {
    private int id;
    private String accountId;
    private String categoryId;
    private String date;
    private double amount;
    private Integer statementMonth;

    private Integer statementYear;
    private Integer oppositeId;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Integer getStatementYear() {
        return statementYear;
    }

    public void setStatementYear(Integer statementYear) {
        this.statementYear = statementYear;
    }

    public Integer getStatementMonth() {
        return statementMonth;
    }

    public void setStatementMonth(Integer statementMonth) { this.statementMonth = statementMonth; }

    public Integer getOppositeTransactionId() {
        return oppositeId;
    }

    public void setOppositeTransactionId(Integer oppositeId) {
        this.oppositeId = oppositeId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FinancialAmount getFinancialAmount() {
        return new FinancialAmount(this.amount);
    }
}
