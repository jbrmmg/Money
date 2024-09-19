package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionFilterDTO {
    private static final int MAXIMUM_PAGE_SIZE = 600;

    private ValueRangeDTO valueRange;
    private DateRangeDTO dateRange;
    private StatementDateDTO statementDate;
    private List<AccountDTO> accounts;
    private List<CategoryDTO> categories;
    private Boolean locked;
    private Boolean predicted;
    private Boolean fromReconciled;
    @Pattern(regexp="^[\\da-zA-Z ]*$",message="Description can only contain digits, letters and spaces.")
    private String description;
    private List<TransactionSortDTO> transactionSorts;
    private Integer maxPageSize;
    private Integer pageNumber;

    public ValueRangeDTO getValueRange() {
        return valueRange;
    }

    public void setValueRange(ValueRangeDTO valueRange) {
        this.valueRange = valueRange;
    }

    public DateRangeDTO getDateRange() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<TransactionSortDTO> getTransactionSorts() {
        // If the sort definition is null or empty then return the minimum.
        if(this.transactionSorts == null || this.transactionSorts.isEmpty()) {
            List<TransactionSortDTO> defaultSorting = new ArrayList<>();
            defaultSorting.add(new TransactionSortDTO(TransactionSortField.DATE,TransactionSortType.ASCENDING));
            defaultSorting.add(new TransactionSortDTO(TransactionSortField.ACCOUNT,TransactionSortType.ASCENDING));
            defaultSorting.add(new TransactionSortDTO(TransactionSortField.STATEMENT,TransactionSortType.ASCENDING));
            defaultSorting.add(new TransactionSortDTO(TransactionSortField.AMOUNT,TransactionSortType.ASCENDING));
            return defaultSorting;
        }

        return transactionSorts;
    }

    public void setTransactionSorts(List<TransactionSortDTO> transactionSorts) {
        this.transactionSorts = transactionSorts;
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

        builder.append(", description: ");
        if(this.getDescription() == null) {
            builder.append("all");
        } else {
            builder.append(this.getDescription());
        }

        return builder.toString();
    }

    public Integer getMaxPageSize() {
        if(this.maxPageSize == null) {
            return MAXIMUM_PAGE_SIZE;
        }

        return maxPageSize;
    }

    public void setMaxPageSize(Integer maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public Integer getPageNumber() {
        if(this.pageNumber == null) {
            return 0;
        }

        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}
