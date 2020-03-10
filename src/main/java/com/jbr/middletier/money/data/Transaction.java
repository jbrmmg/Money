package com.jbr.middletier.money.data;

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.swing.plaf.nimbus.State;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */
@Entity
@Table(name="Transaction")
public class Transaction {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name="account")
    @ManyToOne(optional = false)
    private Account account;

    @JoinColumn(name="category")
    @ManyToOne(optional = false)
    private Category category;

    @Column(name="date")
    private Date date;

    @Column(name="amount")
    private double amount;

    @ManyToOne()
    @JoinColumnsOrFormulas(value = {
            @JoinColumnOrFormula(formula = @JoinFormula(value="account", referencedColumnName = "account")),
            @JoinColumnOrFormula(column = @JoinColumn(name="statement_month", referencedColumnName = "month")),
            @JoinColumnOrFormula(column = @JoinColumn(name="statement_year", referencedColumnName = "year"))
    })
    private Statement statement;

    @Column(name="oppositeid")
    private Integer oppositeId;

    @Column(name="description")
    private String description;

    public Transaction() {
    }

    public Transaction (Account account, Category category, Date date, double amount, String description) throws Exception {
        this.account = account;
        this.category = category;

        this.date = date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.date);
        calendar.set(Calendar.HOUR_OF_DAY,12);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        this.date = calendar.getTime();

        this.amount = amount;

        this.description = description;
    }

    public static final String TransactionDateFormat = "yyyy-MM-dd";

    public int getId() {
        return id;
    }

    public Account getAccount() {
        return this.account;
    }

    public double getAmount() { return this.amount; }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    public Statement getStatement() { return this.statement; }

    public void clearStatement() {
        this.statement = null;
    }

    public boolean reconciled() {

        return (this.statement != null);
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setCategory(Category category) { this.category = category; }

    public Category getCategory() { return this.category; }

    public void setOppositeTransactionId(Integer oppositeTransactionId) { this.oppositeId = oppositeTransactionId; }

    public Integer getOppositeTransactionId() {
        return this.oppositeId;
    }

    public Date getDate() { return this.date; }
}
