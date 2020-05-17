package com.jbr.middletier.money.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * Created by jason on 07/03/17.
 */
@SuppressWarnings("unused")
@Entity
@Table(name="Statement")
public class Statement implements Comparable<Statement> {
    @EmbeddedId
    private StatementId id;

    @Column(name="open_balance")
    private double openBalance;

    @Column(name="locked")
    @NotNull
    private Boolean locked;

    private Statement(StatementId previousId, double balance) {
        // Create next statement in sequence.
        this.id = StatementId.getNextId(previousId);
        this.openBalance = balance;
        this.locked = false;
    }

    public Statement() {
    }

    public Statement(Account account, int month, int year, double openBalance, boolean locked) {
        this.id = new StatementId(account,year,month);
        this.openBalance = openBalance;
        this.locked = locked;
    }

    public StatementId getId() {
        return this.id;
    }

    public double getOpenBalance() {
        return this.openBalance;
    }

    public void setOpenBalance(double openBalance) { this.openBalance = openBalance; }

    public boolean getLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public int compareTo(final Statement o) {
        // First compare the account.
        if(!this.id.getAccount().getId().equalsIgnoreCase(o.id.getAccount().getId())) {
            return this.id.getAccount().getId().compareTo(o.id.getAccount().getId());
        }

        // Then compare the year
        if(!this.id.getYear().equals(o.id.getYear())) {
            return this.id.getYear().compareTo(o.id.getYear());
        }

        // Finally the month
        if(!this.id.getMonth().equals(o.id.getMonth())) {
            return this.id.getMonth().compareTo(o.id.getMonth());
        }

        return 0;
    }

    public Statement lock(double balance) {
        // Lock this statement and create the next in sequence.
        locked = true;

        return new Statement(this.id,balance);
    }
}
