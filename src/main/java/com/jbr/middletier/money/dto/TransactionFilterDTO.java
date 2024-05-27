package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionFilterDTO {
    private ValueRangeDTO valueRange;
    private DateRangeDTO dateRange;
    private StatementDateDTO statementDate;
    private List<AccountDTO> accounts;
    private List<CategoryDTO> categories;
    private Boolean locked;
    private Boolean predicted;
    private Boolean fromReconciled;
    @Pattern(regexp="^[\\da-zA-Z]{4}$",message="Account can only contain letters or digits of 4 characters.")
    private String reconciliationAccount;
    @Pattern(regexp="^[\\da-zA-Z ]*$",message="Description can only contain digits, letters and spaces.")
    private String description;

    public ValueRangeDTO getValueRange() {
        if(this.valueRange == null) {
            return new ValueRangeDTO();
        }

        return valueRange;
    }

    public void setValueRange(ValueRangeDTO valueRange) {
        this.valueRange = valueRange;
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

    public String getReconciliationAccount() {
        return reconciliationAccount;
    }

    public void setReconciliationAccount(String reconciliationAccount) {
        this.reconciliationAccount = reconciliationAccount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Filter: value range: ");
        builder.append(this.valueRange);

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
        if(this.getReconciliationAccount() == null) {
            builder.append("any");
        } else {
            builder.append(this.getReconciliationAccount());
        }

        builder.append(", description: ");
        if(this.getDescription() == null) {
            builder.append("all");
        } else {
            builder.append(this.getDescription());
        }

        return builder.toString();
    }
}
