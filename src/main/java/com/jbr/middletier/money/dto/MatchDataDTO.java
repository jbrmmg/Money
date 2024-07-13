package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class MatchDataDTO {
    private int id;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String date;
    private BigDecimal amount;
    private TransactionDTO transaction;
    private BigDecimal beforeAmount;
    private BigDecimal afterAmount;
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Pattern(regexp="^[\\da-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;
    @Pattern(regexp="^[\\da-zA-Z\\s]{1,40}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    @Pattern(regexp="^\\w{1,40}$",message="Forward Account can only contain letters or digits up to 45 characters.")
    private String forwardAction;
    @Pattern(regexp="^\\w{1,40}$",message="Backward Action can only contain letters or digits up to 45 characters.")
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionDTO getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionDTO transaction) {
        this.transaction = transaction;
    }

    public BigDecimal getBeforeAmount() {
        return beforeAmount;
    }

    public void setBeforeAmount(BigDecimal beforeAmount) {
        this.beforeAmount = beforeAmount;
    }

    public BigDecimal getAfterAmount() {
        return afterAmount;
    }

    public void setAfterAmount(BigDecimal afterAmount) {
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
