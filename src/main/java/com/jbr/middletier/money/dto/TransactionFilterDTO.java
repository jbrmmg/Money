package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionFilterDTO {
    private static final int MAXIMUM_PAGE_SIZE = 600;

    @Getter
    private ValueRangeDTO valueRange;
    @Getter
    private DateRangeDTO dateRange;
    private StatementDateDTO statementDate;
    @Getter
    private Integer statementAge;
    private List<AccountDTO> accounts;
    private List<CategoryDTO> categories;
    @Getter
    private Boolean locked;
    @Getter
    private Boolean predicted;
    @Getter
    private Boolean fromReconciled;
    @Getter
    @Pattern(regexp="^[\\da-zA-Z ]*$",message="Description can only contain digits, letters and spaces.")
    private String description;
    private List<TransactionSortDTO> transactionSorts;
    private Integer maxPageSize;
    private Integer pageNumber;

    public StatementDateDTO getStatementDate() {
        if(this.statementDate == null) {
            return new StatementDateDTO();
        }

        return statementDate;
    }

    public List<AccountDTO> getAccounts() {
        if(this.accounts == null) {
            return new ArrayList<>();
        }

        return accounts;
    }

    public List<CategoryDTO> getCategories() {
        if(this.categories == null) {
            return new ArrayList<>();
        }

        return categories;
    }

    public List<TransactionSortDTO> getTransactionSorts() {
        // If the sort definition is null or empty, then return the minimum.
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

    public Integer getPageNumber() {
        if(this.pageNumber == null) {
            return 0;
        }

        return pageNumber;
    }

}
