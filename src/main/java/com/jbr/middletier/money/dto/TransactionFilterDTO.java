package com.jbr.middletier.money.dto;

import java.util.List;

public class TransactionFilterDTO {
    private ValueRangeDTO debitRange;
    private ValueRangeDTO creditRange;
    private DateRangeDTO dateRange;
    private StatementIdDTO statementId;
    private List<AccountDTO> accounts;
    private List<CategoryDTO> categories;
    private Boolean locked;
    private Boolean predicted;
    private Boolean fromReconciled;

    public ValueRangeDTO getDebitRange() {
        return debitRange;
    }

    public void setDebitRange(ValueRangeDTO debitRange) {
        this.debitRange = debitRange;
    }

    public ValueRangeDTO getCreditRange() {
        return creditRange;
    }

    public void setCreditRange(ValueRangeDTO creditRange) {
        this.creditRange = creditRange;
    }

    public DateRangeDTO getDateRange() {
        return dateRange;
    }

    public void setDateRange(DateRangeDTO dateRange) {
        this.dateRange = dateRange;
    }

    public StatementIdDTO getStatementId() {
        return statementId;
    }

    public void setStatementId(StatementIdDTO statementId) {
        this.statementId = statementId;
    }

    public List<AccountDTO> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountDTO> accounts) {
        this.accounts = accounts;
    }

    public List<CategoryDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<CategoryDTO> categories) {
        this.categories = categories;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Boolean getPredicted() {
        return predicted;
    }

    public void setPredicted(Boolean predicted) {
        this.predicted = predicted;
    }

    public Boolean getFromReconciled() {
        return fromReconciled;
    }

    public void setFromReconciled(Boolean fromReconciled) {
        this.fromReconciled = fromReconciled;
    }
}
