package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class TransactionDTO {
    @Setter
    @Getter
    private int id;
    @Setter
    @Getter
    @Pattern(regexp="^[\\da-zA-Z]{4}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    @Setter
    @Getter
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Setter
    @Getter
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String date;
    @Setter
    @Getter
    private BigDecimal amount;
    @Setter
    @Getter
    private Integer statementMonth;

    @Setter
    @Getter
    private Integer statementYear;
    private Integer oppositeId;
    @Setter
    @Getter
    @Pattern(regexp= "^[\\da-zA-Z ./?\\-*#'&():+-,!]{1,40}$",message="Description can contain only digits, letters or ./?-*#'&():+-,!.")
    private String description;

    @Setter
    @Getter
    private Boolean hasStatement;

    @Setter
    @Getter
    private Boolean statementLocked;

    @Setter
    @Getter
    private String error;

    public Integer getOppositeTransactionId() {
        return oppositeId;
    }

    public void setOppositeTransactionId(Integer oppositeId) {
        this.oppositeId = oppositeId;
    }

    @JsonIgnore
    public FinancialAmount getFinancialAmount() {
        return new FinancialAmount(this.amount);
    }
}
