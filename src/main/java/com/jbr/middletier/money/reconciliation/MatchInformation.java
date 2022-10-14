package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.ReconciliationData;
import com.jbr.middletier.money.data.Transaction;

public class MatchInformation {
    private ReconciliationData reconciliationData;
    private Transaction transaction;
    private long daysAway;

    public MatchInformation() {
        this.reconciliationData = null;
        this.transaction = null;
        this.daysAway = 0;
    }

    public ReconciliationData getReconciliationData() {
        return reconciliationData;
    }

    public void setReconciliationData(ReconciliationData reconciliationData) {
        this.reconciliationData = reconciliationData;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public long getDaysAway() {
        return daysAway;
    }

    public void setDaysAway(long daysAway) {
        this.daysAway = daysAway;
    }

    public boolean exactMatch() {
        return this.reconciliationData != null && this.daysAway == 0;
    }

    public boolean closeMatch() {
        return this.reconciliationData != null && this.daysAway != 0;
    }
}
