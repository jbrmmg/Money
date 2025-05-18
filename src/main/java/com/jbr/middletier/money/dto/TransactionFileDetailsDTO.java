package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TransactionFileDetailsDTO {
    List<TransactionDTO> transactions;
    @Setter
    boolean ok;
    @Setter
    @Pattern(regexp="^[\\da-zA-Z]{1,100}$",message="Error can only contain letters or digits up to 100 characters.")
    String error;
    @Setter
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Account can only contain letters or digits of 4 characters.")
    String accountId;

    public TransactionFileDetailsDTO() {
        this.transactions = new ArrayList<>();
        this.ok = false;
        this.error = "Uninitialised";
        this.accountId = null;
    }

    public void addTransaction(TransactionDTO transaction) {
        this.transactions.add(transaction);
    }
}
