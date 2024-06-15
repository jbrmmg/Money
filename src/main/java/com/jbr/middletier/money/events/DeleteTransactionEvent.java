package com.jbr.middletier.money.events;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class DeleteTransactionEvent extends ApplicationEvent {
    private final List<Integer> transactionIds;

    public DeleteTransactionEvent(Object source, List<Integer> transactionIds) {
        super(source);
        this.transactionIds = transactionIds;
    }

    public List<Integer> getTransactionIds() {
        return this.transactionIds;
    }
}
