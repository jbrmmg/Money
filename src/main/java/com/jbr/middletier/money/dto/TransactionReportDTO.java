package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.validation.constraints.Pattern;

public class TransactionReportDTO {
    private TransactionReportTypeDTO type;
    private Integer id;
    private Integer transactionId;
    private FinancialAmount amount;
    private FinancialAmount balance;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String date;
    private AccountDTO account;
    private CategoryDTO category;
    @Pattern(regexp="^[\\da-zA-Z\\s]{1,40}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;
    private Integer oppositeId;
    private StatementDTO statement;
    private Boolean predicted;
    private Boolean fromReconciliation;
    private Boolean actionUpdate;
    private Boolean actionReconcile;
    private Boolean actionUnreconcile;
    private Boolean actionDelete;

    public TransactionReportTypeDTO getType() {
        return type;
    }

    public void setType(TransactionReportTypeDTO type) {
        this.type = type;
    }

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

    public Boolean getActionUpdate() {
        return actionUpdate;
    }

    public void setActionUpdate(Boolean actionUpdate) {
        this.actionUpdate = actionUpdate;
    }

    public Boolean getActionReconcile() {
        return actionReconcile;
    }

    public void setActionReconcile(Boolean actionReconcile) {
        this.actionReconcile = actionReconcile;
    }

    public Boolean getActionUnreconcile() {
        return actionUnreconcile;
    }

    public void setActionUnreconcile(Boolean actionUnreconcile) {
        this.actionUnreconcile = actionUnreconcile;
    }

    public Boolean getActionDelete() {
        return actionDelete;
    }

    public void setActionDelete(Boolean actionDelete) {
        this.actionDelete = actionDelete;
    }

    @Override
    public String toString() {

        return "[" +
                this.getId() +
                " " +
                this.getDate() +
                " " +
                this.getAmount().getValue() +
                " " +
                this.getPredicted() +
                " " +
                this.getFromReconciliation() +
                " " +
                "]";
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }
}
