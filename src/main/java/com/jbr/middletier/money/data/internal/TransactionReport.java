package com.jbr.middletier.money.data.internal;

import jakarta.persistence.*;

@Entity
@Table(name="transaction_report")
public class TransactionReport {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name="transaction_id")
    private Integer transactionId;

    @Column(name="amount")
    private Double amount;

    @Column(name="date")
    private String date;

    @Column(name="account_id")
    private String accountId;

    @Column(name="category_id")
    private String categoryId;

    @Column(name="description")
    private String description;

    @Column(name="search_description")
    private String searchDescription;

    @Column(name="opposite_id")
    private Integer oppositeId;

    @Column(name="statement_open_balance")
    private Double statementOpenBalance;

    @Column(name="statement_month")
    private Integer statementMonth;

    @Column(name="statement_year")
    private Integer statementYear;

    @Column(name="statement_sort")
    private Integer statementSort;

    @Column(name="predicted")
    private Boolean predicted;

    @Column(name="from_reconciliation")
    private Boolean fromReconciliation;

    @Column(name="locked")
    private Boolean locked;

    @Column(name="action_update_category")
    private Boolean actionUpdateCategory;

    @Column(name="action_update")
    private Boolean actionUpdate;

    @Column(name="action_reconcile")
    private Boolean actionReconcile;

    @Column(name="action_unreconcile")
    private Boolean actionUnreconcile;

    @Column(name="action_delete")
    private Boolean actionDelete;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getAmount() {
        return amount;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setOppositeId(Integer oppositeId) {
        this.oppositeId = oppositeId;
    }

    public Integer getOppositeId() {
        return oppositeId;
    }

    public void setStatementMonth(Integer statementMonth) {
        this.statementMonth = statementMonth;
    }

    public Integer getStatementMonth() {
        return statementMonth;
    }

    public void setStatementYear(Integer statementYear) {
        this.statementYear = statementYear;
    }

    public Integer getStatementYear() {
        return statementYear;
    }

    public Boolean getPredicted() {
        return predicted;
    }

    public void setPredicted(Boolean predicted) {
        this.predicted = predicted;
    }

    public void setFromReconciliation(Boolean fromReconciliation) {
        this.fromReconciliation = fromReconciliation;
    }

    public Boolean getFromReconciliation() {
        return fromReconciliation;
    }

    public Double getStatementOpenBalance() {
        return statementOpenBalance;
    }

    public void setStatementOpenBalance(Double statementOpenBalance) {
        this.statementOpenBalance = statementOpenBalance;
    }

    public Boolean getActionUpdateCategory() {
        return actionUpdateCategory;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setActionUpdateCategory(Boolean actionUpdateCategory) {
        this.actionUpdateCategory = actionUpdateCategory;
    }

    public void setActionUpdate(Boolean actionUpdate) {
        this.actionUpdate = actionUpdate;
    }

    public Boolean getActionUpdate() {
        return actionUpdate;
    }

    public void setActionReconcile(Boolean actionReconcile) {
        this.actionReconcile = actionReconcile;
    }

    public Boolean getActionReconcile() {
        return actionReconcile;
    }

    public void setActionUnreconcile(Boolean actionUnreconcile) {
        this.actionUnreconcile = actionUnreconcile;
    }

    public Boolean getActionUnreconcile() {
        return actionUnreconcile;
    }

    public void setActionDelete(Boolean actionDelete) {
        this.actionDelete = actionDelete;
    }

    public Boolean getActionDelete() {
        return actionDelete;
    }

    public void setSearchDescription(String searchDescription) {
        this.searchDescription = searchDescription;
    }

    public String getSearchDescription() {
        return searchDescription;
    }

    public void setStatementSort(Integer statementSort) {
        this.statementSort = statementSort;
    }

    public Integer getStatementSort() {
        return statementSort;
    }
}
