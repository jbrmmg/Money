package com.jbr.middletier.money.reporting;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
public class TransactionRow {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final String date;
    private final String accountName;
    private final String categoryColour;
    private final String categoryName;
    private final String description;
    private final String amountFormatted;
    private final boolean debit;
    private final boolean transfer;

    public TransactionRow(LocalDate date,
                          String accountName,
                          String categoryColour,
                          String categoryName,
                          String description,
                          BigDecimal amount,
                          boolean transfer) {
        this.date = DATE_FMT.format(date);
        this.accountName = accountName;
        this.categoryColour = categoryColour != null ? categoryColour : "999999";
        this.categoryName = categoryName;
        this.description = description != null ? description : "";
        this.debit = amount.compareTo(BigDecimal.ZERO) < 0;
        this.amountFormatted = String.format("£%,.2f", amount.abs());
        this.transfer = transfer;
    }
}
