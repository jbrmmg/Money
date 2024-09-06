package com.jbr.middletier.money.events;

import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.dto.StatementDTO;
import org.springframework.context.ApplicationEvent;

public class StatementLockEvent extends ApplicationEvent {
    private final Statement statement;
    private final Account account;
    private final StatementDTO penultimateStatement;

    public StatementLockEvent(Object source, Statement statement) {
        super(source);
        this.statement = statement;
        this.account = null;
        this.penultimateStatement = null;
    }

    public StatementLockEvent(Object source, Account account, StatementDTO penultimateStatement) {
        super(source);
        this.statement = null;
        this.account = account;
        this.penultimateStatement = penultimateStatement;
    }

    public Statement getStatement() {
        return this.statement;
    }

    public Account getAccount() {
        return this.account;
    }

    public StatementDTO getPenultimateStatement() {
        return this.penultimateStatement;
    }
}
