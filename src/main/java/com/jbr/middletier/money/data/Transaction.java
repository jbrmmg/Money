package com.jbr.middletier.money.data;

import javax.persistence.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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

    @Column(name="account")
    private String account;

    @Column(name="category")
    private String category;

    @Column(name="date")
    private Date date;

    @Column(name="amount")
    private double amount;

    @Column(name="statement")
    private String statement;

    @Column(name="oppositeid")
    private Integer oppositeiId;

    @Column(name="description")
    private String description;

    public Transaction() {
    }

    public Transaction (NewTransaction newTransaction) throws ParseException {
        this.account = newTransaction.getAccount();
        this.category = newTransaction.getCategory();

        SimpleDateFormat formatter = new SimpleDateFormat(Transaction.TransactionDateFormat);
        this.date = formatter.parse(newTransaction.getDate());

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.date);
        calendar.set(Calendar.HOUR_OF_DAY,12);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        this.date = calendar.getTime();

        this.amount = newTransaction.getAmount();

        this.description = newTransaction.getDescription();
    }

    public static final String TransactionDateFormat = "yyyy-MM-dd";

    public int getId() {
        return id;
    }

    public String getAccount() {
        return this.account;
    }

    public double getAmount() { return this.amount; }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() { return this.description; }

    public void setDescription(String description) { this.description = description; }

    public void setStatement(Statement statement) {
        this.statement = statement.getYearMonthId();
    }

    public void clearStatement() {
        this.statement = null;
    }

    public boolean reconciled() {
        return (this.statement != null && this.statement.length() > 0);
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setCategory(String category) { this.category = category; }

    public String getCategory() { return this.category; }

    public void setOppositeId(int oppositeId) {
        this.oppositeiId = oppositeId;
    }

    public boolean hasOppositeId() {
        return this.oppositeiId != null;
    }

    public Date getDate() { return this.date; }

    public int getOppositeId() {
        return ( this.oppositeiId == null ) ? -1 : this.oppositeiId;
    }

    public StatementId calculateStatementId() {
        // Get the statement id.
        int year = 0;
        int month = 0;
        if(this.statement != null && this.statement.length() >= 6) {
            year = Integer.parseInt(this.statement.substring(0,4));
            month = Integer.parseInt(this.statement.substring(4,6));
        } else {
            return null;
        }

        return new StatementId(this.account,year,month);
    }
}
