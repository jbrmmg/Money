package com.jbr.middletier.money.dto;

@SuppressWarnings("unused")
public class ReconciliationFileDTO {
    private String filename;
    private String accountId;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
