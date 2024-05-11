package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.validation.constraints.Pattern;

public class TransactionDTO {
    private int id;
    @Pattern(regexp="^[0-9a-zA-Z]{4}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Pattern(regexp = "^[0-9]{4}-[0-9]{2}-[0-9]{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String date;
    private double amount;
    private Integer statementMonth;

    private Integer statementYear;
    private Integer oppositeId;
    @Pattern(regexp="^[0-9a-zA-Z\\s!]{1,45}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;

    private Boolean hasStatement;

    private Boolean statementLocked;

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

    public Boolean getHasStatement() {
        return hasStatement;
    }

    public void setHasStatement(Boolean hasStatement) {
        this.hasStatement = hasStatement;
    }

    public Boolean getStatementLocked() {
        return statementLocked;
    }

    public void setStatementLocked(Boolean statementLocked) {
        this.statementLocked = statementLocked;
    }

    public FinancialAmount getFinancialAmount() {
        return new FinancialAmount(this.amount);
    }
}
