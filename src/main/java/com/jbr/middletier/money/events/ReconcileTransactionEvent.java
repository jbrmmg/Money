package com.jbr.middletier.money.events;

import com.jbr.middletier.money.data.primary.Transaction;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class ReconcileTransactionEvent extends ApplicationEvent {
    private final List<Transaction> transactions;

    public ReconcileTransactionEvent(Object source, List<Transaction> transactions) {
        super(source);
        this.transactions = transactions;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }
}
