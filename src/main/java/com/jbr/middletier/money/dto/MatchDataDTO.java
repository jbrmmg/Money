package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

public class MatchDataDTO {
    private int id;
    @Pattern(regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String date;
    private double amount;
    private TransactionDTO transaction;
    private double beforeAmount;
    private double afterAmount;
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Pattern(regexp="^[0-9a-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;
    @Pattern(regexp="^[0-9a-zA-Z\\s]{1,40}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    @Pattern(regexp="^[0-9a-zA-Z_]{1,40}$",message="Forward Account can only contain letters or digits up to 45 characters.")
    private String forwardAction;
    @Pattern(regexp="^[0-9a-zA-Z_]{1,40}$",message="Backward Action can only contain letters or digits up to 45 characters.")
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

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
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
