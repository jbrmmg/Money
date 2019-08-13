package com.jbr.middletier.money.data;

public class ReconcileTransaction {
    private int transactionId;
    private boolean reconcile;

    public ReconcileTransaction() {
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
