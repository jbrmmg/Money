package com.jbr.middletier.money.data;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jason on 07/03/17.
 */
@Entity
@Table(name="Statement")
@IdClass(StatementId.class)
public class Statement implements Comparable<Statement> {
    @Id
    @NotNull
    @Column(name="account")
    private String account;

    @Id
    @NotNull
    @Column(name="month")
    private Integer month;

    @Id
    @Column(name="year")
    private Integer year;

    @Column(name="open_balance")
    private double openBalance;

    @Column(name="locked")
    @NotNull
    private Boolean locked;

    private Statement(String account, int year, int month, double balance) {
        // Create next statement in sequence.
        this.account = account;
        this.openBalance = balance;
        this.locked = false;

        if(month == 12) {
            this.month = 1;
            this.year = year + 1;
        } else {
            this.month = month + 1;
            this.year = year;
        }
    }

    public Statement() {
    }

    public Statement(String account, int month, int year, double openBalance, boolean locked) {
        this.account = account;
        this.month = month;
        this.year = year;
        this.openBalance = openBalance;
        this.locked = locked;
    }

    public String getYearMonthId() {
        int yearMonth = this.year * 100 + this.month;
        return Integer.toString(yearMonth);
    }

    public int getYear() {
        return this.year;
    }

    public void setYear(int year) {this.year = year;}

    public int getMonth() {
        return this.month;
    }

    public void setMonth(int month) { this.month = month;}

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) { this.account = account; }

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
        if(!this.account.equalsIgnoreCase(o.account)) {
            return this.account.compareTo(o.account);
        }

        // Then compare the year
        if(!this.year.equals(o.year)) {
            return this.year.compareTo(o.year);
        }

        // Finally the month
        if(!this.month.equals(o.month)) {
            return this.month.compareTo(o.month);
        }

        return 0;
    }

    public Statement lock(double balance) {
        // Lock this statement and create the next in sequence.
        locked = true;

        return new Statement(this.account,this.year,this.month,balance);
    }

    public static String getIdFromDateString(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMM");
        return formatter.format(date);
    }
}
