package com.jbr.middletier.money.data;

import liquibase.pro.packaged.C;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by jason on 11/04/17.
 */
@Entity
@Table(name="ReconciliationData")
public class ReconciliationData {
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JoinColumn(name="category")
    @ManyToOne
    private Category category;

    @Column(name="description", length = 40)
    private String description;

    @Column(name="date")
    private Date date;

    @Column(name="amount")
    private double amount;

    public ReconciliationData() {
    }

    public ReconciliationData(Date date, double amount, Category category, String description) {
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof Transaction)) {
            return false;
        }

        Transaction transaction = (Transaction) o;

        return transaction.getAmount() == this.amount && transaction.getDate().equals(this.date);
    }

    public long closeMatch(Transaction transaction) {
        if(transaction.getAmount() != this.amount) {
            return -1;
        }

        // Subtract one date from the other.
        long difference = Math.abs(this.date.getTime() - transaction.getDate().getTime());
        return TimeUnit.DAYS.convert(difference,TimeUnit.MILLISECONDS);
    }

    public int getId() {
        return this.id;
    }

    public Date getDate() {
        return this.date;
    }

    public double getAmount() {
        return this.amount;
    }

    public Category getCategory() { return this.category; }

    public void setCategory(Category category, String colour) {
        this.category = category;
    }

    public String getDescription() { return this.description; }
}
