package com.jbr.middletier.money.dto;

import java.util.ArrayList;
import java.util.List;

public class ReconcileTransactionDTO {
    private List<Integer> transactions;
    private boolean reconcile;

    public ReconcileTransactionDTO() {
        this.transactions = new ArrayList<>();
        this.reconcile = false;
    }

    public List<Integer> getTransactions() {
        return this.transactions;
    }

    public void setTransactions(List<Integer> transactions) {
        this.transactions = transactions;
    }

    public boolean getReconcile() {
        return this.reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }
}
