package com.jbr.middletier.money.dto;

import java.time.LocalDateTime;

public class ReconciliationFileDTO {
    private String filename;
    private AccountDTO account;
    private LocalDateTime lastModified;
    private Long size;
    private String error;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
