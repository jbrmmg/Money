package com.jbr.middletier.money.data;

import javax.persistence.*;
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

    @Column(name="category")
    private String category;

    @Column(name="colour")
    private String colour;

    @Column(name="description", length = 40)
    private String description;

    @Column(name="date")
    private Date date;

    @Column(name="amount")
    private double amount;

    public ReconciliationData() {
    }

    public ReconciliationData(Date date, double amount, String category, String colour, String description) {
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
        this.colour = colour;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof AllTransaction)) {
            return false;
        }

        AllTransaction transaction = (AllTransaction) o;

        return transaction.getAmount() == this.amount && transaction.getDate().equals(this.date);
    }

    public long closeMatch(AllTransaction transaction) {
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

    public String getCategory() { return this.category; }

    public void setCategory(String category, String colour) {
        this.category = category;
        this.colour = colour;
    }

    public String getColour() { return this.colour; }

    public String getDescription() { return this.description; }
}
