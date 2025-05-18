package com.jbr.middletier.money.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class DeleteTransactionEvent extends ApplicationEvent {
    private final List<Integer> transactionIds;

    public DeleteTransactionEvent(Object source, List<Integer> transactionIds) {
        super(source);
        this.transactionIds = transactionIds;
    }
}
