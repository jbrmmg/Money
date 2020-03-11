package com.jbr.middletier.money.data;

import javax.mail.internet.InternetAddress;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by jason on 07/03/17.
 */

@Embeddable
public class StatementId implements Serializable {
    @NotNull
    @ManyToOne
    @JoinColumn(name="account")
    private Account account;

    @NotNull
    @Column(name="month")
    private Integer month;

    @NotNull
    @Column(name="year")
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

    public Account getAccount() { return this.account; }

    public void setAccount(Account account) { this.account = account; }

    public Integer getMonth() { return this.month; }

    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return this.year; }

    public void setYear(Integer year) { this.year = year; }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (!(o instanceof StatementId)) {
            return false;
        }

        StatementId statementId = (StatementId)o;

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
