package com.jbr.middletier.money.data.primary;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Created by jason on 07/03/17.
 */

@Setter
@Getter
@SuppressWarnings("WeakerAccess")
@Embeddable
public class StatementId implements Serializable {
    @NotNull
    @ManyToOne
    @JoinColumn(name="account")
    private Account account;

    @NotNull
    @Column(name="month_val")
    private Integer month;

    @NotNull
    @Column(name="year_val")
    private Integer year;

    public StatementId(Account account, int year, int month) {
        this.account = account;
        this.month = month;
        this.year = year;
    }

    public static StatementId getNextId(StatementId sourceId) {
        StatementId result = new StatementId();

        result.account = sourceId.account;

        if(sourceId.month == 12) {
            result.month = 1;
            result.year = sourceId.year + 1;
        } else {
            result.month = sourceId.month + 1;
            result.year = sourceId.year;
        }

        return result;
    }

    public static StatementId getPreviousId(StatementId sourceId) {
        StatementId result = new StatementId();

        result.account = sourceId.account;

        if(sourceId.month == 1) {
            result.month = 12;
            result.year = sourceId.year - 1;
        } else {
            result.month = sourceId.month - 1;
            result.year = sourceId.year;
        }

        return result;
    }

    public StatementId(){}

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof StatementId statementId)) {
            return false;
        }

        return this.toString().equalsIgnoreCase(statementId.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return this.account.getId().toUpperCase() + String.format("%04d",this.year) + String.format("%02d",this.month);
    }
}
