package com.jbr.middletier.money.data;

/**
 * Created by jason on 11/03/17.
 */
public class NewTransaction {
    private String account;
    private String category;
    private String date;
    private double amount;
    private boolean accountTransfer;
    private String transferAccount;


    public NewTransaction() {
    }

    public NewTransaction(String account, String category, String date, double amount) {
        this.account = account;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = false;
        this.transferAccount = "";
    }

    public NewTransaction(String account, String category, String date, double amount, String transferAccount) {
        this.account = account;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.accountTransfer = true;
        this.transferAccount = transferAccount;
    }

    public NewTransaction(MatchData matchData)
    {
        this.account = matchData.getAccount();
        this.category = matchData.getCategory();
        this.date = matchData.getDate();
        this.amount = matchData.getAmount();
        this.accountTransfer = false;
        this.transferAccount = "";
    }

    public String getAccount() {
        return this.account;
    }

    public String getCategory() {
        if(this.accountTransfer)
            return "TRF";

        return this.category;
    }

    public String getDate() {
        return this.date;
    }

    public double getAmount() {
        return this.amount;
    }

    public String getTransferAccount() {
        return this.transferAccount;
    }

    public boolean isAccountTransfer() {
        return this.accountTransfer;
    }
}
