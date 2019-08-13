package com.jbr.middletier.money.data;

public class LockStatementRequest {
    private String accountId;
    private int year;
    private int month;

    public LockStatementRequest() {
        this.accountId = "UNKN";
        this.year = 1900;
        this.year = 1;
    }

    public int getMonth() {
        return this.month;
    }

    public int getYear() {
        return this.year;
    }

    public String getAccountId() {
        return this.accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
