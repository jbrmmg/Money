package com.jbr.middletier.money.dto;

import java.util.ArrayList;
import java.util.List;

public class TransactionFilterDTO {
    private ValueRangeDTO debitRange;
    private ValueRangeDTO creditRange;
    private DateRangeDTO dateRange;
    private StatementDateDTO statementDate;
    private List<AccountDTO> accounts;
    private List<CategoryDTO> categories;
    private Boolean locked;
    private Boolean predicted;
    private Boolean fromReconciled;

    public ValueRangeDTO getDebitRange() {
        if(this.debitRange == null) {
            return new ValueRangeDTO();
        }

        return debitRange;
    }

    public void setDebitRange(ValueRangeDTO debitRange) {
        this.debitRange = debitRange;
    }

    public ValueRangeDTO getCreditRange() {
        if(this.creditRange == null) {
            return new ValueRangeDTO();
        }

        return creditRange;
    }

    public void setCreditRange(ValueRangeDTO creditRange) {
        this.creditRange = creditRange;
    }

    public DateRangeDTO getDateRange() {
        if(this.dateRange == null) {
            return new DateRangeDTO();
        }

        return dateRange;
    }

    public void setDateRange(DateRangeDTO dateRange) {
        this.dateRange = dateRange;
    }

    public StatementDateDTO getStatementDate() {
        if(this.statementDate == null) {
            return new StatementDateDTO();
        }

        return statementDate;
    }

    public void setStatementDate(StatementDateDTO statementDate) {
        this.statementDate = statementDate;
    }

    public List<AccountDTO> getAccounts() {
        if(this.accounts == null) {
            return new ArrayList<>();
        }

        return accounts;
    }

    public void setAccounts(List<AccountDTO> accounts) {
        this.accounts = accounts;
    }

    public List<CategoryDTO> getCategories() {
        if(this.categories == null) {
            return new ArrayList<>();
        }

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Filter: debitRange: ");
        builder.append(this.getDebitRange());

        builder.append(", creditRange: ");
        builder.append(this.getCreditRange());

        builder.append(", dateRange: ");
        builder.append(this.getDateRange());

        builder.append(", statementDate: ");
        builder.append(this.getStatementDate());

        builder.append(", accounts: (");
        if(!this.getAccounts().isEmpty()) {
            builder.append(String.join(",",this.getAccounts().stream()
                    .map(AccountDTO::getId)
                    .toList()));
        } else {
            builder.append("any");
        }
        builder.append(")");

        builder.append(", categories: (");
        if(!this.getCategories().isEmpty()) {
            builder.append(String.join(",",this.getCategories().stream()
                    .map(CategoryDTO::getId)
                    .toList()));
        } else {
            builder.append("any");
        }
        builder.append(")");

        builder.append(", locked: ");
        if(this.getLocked() == null) {
            builder.append("any");
        } else {
            builder.append(this.getLocked());
        }

        builder.append(", predicted: ");
        if(this.getPredicted() == null) {
            builder.append("any");
        } else {
            builder.append(this.getPredicted());
        }

        builder.append(", from Reconciled: ");
        if(this.getFromReconciled() == null) {
            builder.append("any");
        } else {
            builder.append(this.getFromReconciled());
        }

        return builder.toString();
    }
}
