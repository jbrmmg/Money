package com.jbr.middletier.money.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
public class ReconcileTransactionDTO {
    @Getter
    private List<Integer> transactions;
    private boolean reconcile;

    public ReconcileTransactionDTO() {
        this.transactions = new ArrayList<>();
        this.reconcile = false;
    }

    public boolean getReconcile() {
        return this.reconcile;
    }

}
