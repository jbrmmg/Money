package com.jbr.middletier.money.dto;

public class ReconcileTransactionDTO {
    private int transactionId;
    private boolean reconcile;

    public ReconcileTransactionDTO() {
        this.transactionId = -1;
        this.reconcile = false;
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public void setId(int id) {
        this.transactionId = id;
    }

    public boolean getReconcile() {
        return this.reconcile;
    }

    public void setReconcile(boolean reconcile) {
        this.reconcile = reconcile;
    }
}
