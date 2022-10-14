package com.jbr.middletier.money.data;

import com.jbr.middletier.money.util.TransactionString;

import javax.persistence.*;
import java.time.LocalDate;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Created by jason on 11/04/17.
 */
@SuppressWarnings("unused")
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
    private LocalDate date;

    @Column(name="amount")
    private double amount;

    public ReconciliationData() {
    }

    public ReconciliationData(LocalDate date, double amount, Category category, String description) {
        this.amount = amount;
        this.date = date;
        this.category = category;
        this.description = description;
    }

    private long dateDifferenceWithoutTime(LocalDate leftSide, LocalDate rightSide) {
        try {
            return DAYS.between(leftSide,rightSide);
        } catch(Exception e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof ReconciliationData)) {
            return false;
        }

        ReconciliationData reconciliationData = (ReconciliationData) o;

        return this.toString().equals(reconciliationData.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return TransactionString.formattedTransactionString(this.date,this.amount);
    }

    public long closeMatch(Transaction transaction) {
        if(transaction.getAmount().getValue() != this.amount) {
            return -1;
        }

        // Subtract one date from the other.
        return dateDifferenceWithoutTime(this.date,transaction.getDate());
    }

    public int getId() {
        return this.id;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(LocalDate date) { this.date = date; }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) { this.amount = amount; }

    public Category getCategory() { return this.category; }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() { return this.description; }

    public void setDescription(String description) {
        this.description = description;
    }
}
