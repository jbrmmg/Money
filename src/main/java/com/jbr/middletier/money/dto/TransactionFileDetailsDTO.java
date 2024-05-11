package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

public class TransactionFileDetailsDTO {
    List<TransactionDTO> transactions;
    boolean ok;
    @Pattern(regexp="^[0-9a-zA-Z]{1,100}$",message="Error can only contain letters or digits up to 100 characters.")
    String error;
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Account can only contain letters or digits of 4 characters.")
    String accountId;

    public TransactionFileDetailsDTO() {
        this.transactions = new ArrayList<>();
        this.ok = false;
        this.error = "Uninitialised";
        this.accountId = null;
    }

    public List<TransactionDTO> getTransactions() {
        return this.transactions;
    }

    public void addTransaction(TransactionDTO transaction) {
        this.transactions.add(transaction);
    }

    public void setOk(boolean OK) {
        this.ok = OK;
    }

    public boolean isOk() {
        return this.ok;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getError() {
        return this.error;
    }

    public void setAccountId(String account) {
        this.accountId = account;
    }

    public String getAccountId() {
        return this.accountId;
    }
}
