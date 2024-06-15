package com.jbr.middletier.money.events;

import org.springframework.context.ApplicationEvent;

public class ReconciliationFileLoadEvent extends ApplicationEvent {
    public ReconciliationFileLoadEvent(Object source) {
        super(source);
    }
}
