package com.jbr.middletier.money.data.primary;

import com.jbr.middletier.money.util.FinancialAmount;

import jakarta.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="statement")
public class Statement implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private StatementId id;

    @Column(name="open_balance")
    private double openBalance;

    @Column(name="locked")
    @NotNull
    private Boolean locked;

    private Statement(StatementId previousId, FinancialAmount balance) {
        // Create next statement in sequence.
        this.id = StatementId.getNextId(previousId);
        this.openBalance = balance.getValue();
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

    public void setId(StatementId id) { this.id = id; }

    public FinancialAmount getOpenBalance() {
        return new FinancialAmount(this.openBalance);
    }

    public void setOpenBalance(double openBalance) { this.openBalance = openBalance; }

    public boolean getLocked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Statement lock(FinancialAmount balance) {
        // Lock this statement and create the next in sequence.
        locked = true;

        return new Statement(this.id,balance);
    }
}
