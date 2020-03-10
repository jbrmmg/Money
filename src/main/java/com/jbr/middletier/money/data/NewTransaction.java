package com.jbr.middletier.money.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jason on 11/03/17.
 */
public class NewTransaction {
    private String accountId;
    private String categoryId;
    private Date date;
    private double amount;
    private boolean accountTransfer;
    private String transferAccount;
    private String description;

    public NewTransaction() {
    }

    public NewTransaction(String accountId, String categoryId, Date date, double amount, String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = false;
        this.transferAccount = "";
        this.description = description;
    }

    public NewTransaction(String accountId, String categoryId, Date date, double amount, String transferAccount, String description) {
        this.accountId = accountId;
        this.categoryId = categoryId;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = true;
        this.transferAccount = transferAccount;
        this.description = description;
    }

    public NewTransaction(MatchData matchData) throws ParseException {
        this.accountId = matchData.getAccount().getId();
        this.categoryId = matchData.getCategory().getId();

        SimpleDateFormat formatter = new SimpleDateFormat(Transaction.TransactionDateFormat);
        this.date = formatter.parse(matchData.getDate());

        this.amount = matchData.getAmount();
        this.accountTransfer = false;
        this.transferAccount = "";
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

    public Date getDate() {
        return this.date;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getTransferAccount() {
        return this.transferAccount;
    }

    public String getDescription() { return this.description; }

    public boolean isAccountTransfer() {
        return this.accountTransfer;
    }
}
