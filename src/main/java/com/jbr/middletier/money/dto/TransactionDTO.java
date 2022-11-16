package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;

import java.time.LocalDate;


public class TransactionDTO {
    private int id;
    private AccountDTO account;
    private CategoryDTO category;
    private LocalDate date;
    private double amount;
    private StatementDTO statement;
    private Integer oppositeId;
    private String description;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public CategoryDTO getCategory() {
        return category;
    }

    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public StatementDTO getStatement() {
        return statement;
    }

    public void setStatement(StatementDTO statement) {
        this.statement = statement;
    }

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
