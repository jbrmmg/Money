package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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
    @Pattern(regexp= "^[\\da-zA-Z ./?\\-*#'&():+-,!]{1,40}$",message="Description can contain only digits, letters or ./?-*#'&():+-,!.")
    private String description;
    private Integer oppositeId;
    private StatementDTO statement;
    private Boolean predicted;
    private Boolean fromReconciliation;
    private Boolean actionUpdate;
    private Boolean actionReconcile;
    private Boolean actionUnreconcile;
    private Boolean actionDelete;

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

}
