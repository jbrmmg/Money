package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;

public class TransactionReportDTO {
    private Integer id;
    private FinancialAmount amount;
    private FinancialAmount balance;
    private String date;
    private AccountDTO account;
    private CategoryDTO category;
    private String description;
    private Integer oppositeId;
    private StatementDTO statement;
    private Boolean predicted;
    private Boolean fromReconciliation;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public FinancialAmount getAmount() {
        return amount;
    }

    public void setAmount(FinancialAmount amount) {
        this.amount = amount;
    }

    public FinancialAmount getBalance() {
        return balance;
    }

    public void setBalance(FinancialAmount balance) {
        this.balance = balance;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOppositeId() {
        return oppositeId;
    }

    public void setOppositeId(Integer oppositeId) {
        this.oppositeId = oppositeId;
    }

    public Boolean getPredicted() {
        return predicted;
    }

    public void setPredicted(Boolean predicted) {
        this.predicted = predicted;
    }

    public Boolean getFromReconciliation() {
        return fromReconciliation;
    }

    public void setFromReconciliation(Boolean fromReconciliation) {
        this.fromReconciliation = fromReconciliation;
    }

    public StatementDTO getStatement() {
        return statement;
    }

    public void setStatement(StatementDTO statement) {
        this.statement = statement;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append("[");
        result.append(this.getId());
        result.append(" ");
        result.append(this.getDate());
        result.append(" ");
        result.append(this.getAmount().getValue());
        result.append(" ");
        result.append(this.getPredicted());
        result.append(" ");
        result.append(this.getFromReconciliation());
        result.append(" ");
        result.append("]");

        return result.toString();
    }
}
