package com.jbr.middletier.money.data;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Created by jason on 11/03/17.
 */
@SuppressWarnings("unused")
public class NewTransaction {
    private String accountId;
    private String categoryId;
    private LocalDate date;
    private double amount;
    private boolean accountTransfer;
    private String transferAccountId;
    private String description;

    public NewTransaction() {
    }

    public NewTransaction(String accountId, String categoryId, LocalDate date, double amount, String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = false;
        this.transferAccountId = "";
        this.description = description;
    }

    public NewTransaction(String accountId, String categoryId, LocalDate date, double amount, String transferAccount, String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = true;
        this.transferAccountId = transferAccount;
        this.description = description;
    }

    public NewTransaction(MatchData matchData) throws ParseException {
        this.accountId = matchData.getAccount().getId();
        this.categoryId = matchData.getCategory().getId();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Transaction.TRANSACTION_DATE_FORMAT);
        this.date = LocalDate.parse(matchData.getDate(),formatter);

        this.amount = matchData.getAmount();
        this.accountTransfer = false;
        this.transferAccountId = "";
        this.description = matchData.getDescription();
    }

    public String getAccountId() {
        return this.accountId;
    }

    public String getCategoryId() {
        if(this.accountTransfer)
            return "TRF";

        return this.categoryId;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getTransferAccountId() {
        return this.transferAccountId;
    }

    public String getDescription() { return this.description; }

    public boolean isAccountTransfer() {
        return this.accountTransfer;
    }
}
