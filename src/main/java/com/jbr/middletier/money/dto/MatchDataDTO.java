package com.jbr.middletier.money.dto;

public class MatchDataDTO {
    private int id;
    private String date;
    private double amount;
    private TransactionDTO transaction;
    private double beforeAmount;
    private double afterAmount;
    private String categoryId;
    private String description;
    private String accountId;
    private String forwardAction;
    private String backwardAction;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public TransactionDTO getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionDTO transaction) {
        this.transaction = transaction;
    }

    public double getBeforeAmount() {
        return beforeAmount;
    }

    public void setBeforeAmount(double beforeAmount) {
        this.beforeAmount = beforeAmount;
    }

    public double getAfterAmount() {
        return afterAmount;
    }

    public void setAfterAmount(double afterAmount) {
        this.afterAmount = afterAmount;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getForwardAction() {
        return forwardAction;
    }

    public void setForwardAction(String forwardAction) {
        this.forwardAction = forwardAction;
    }

    public String getBackwardAction() {
        return backwardAction;
    }

    public void setBackwardAction(String backwardAction) {
        this.backwardAction = backwardAction;
    }
}
