package com.jbr.middletier.money.data;

import com.jbr.middletier.money.util.TransactionString;

import javax.persistence.*;
import java.time.LocalDate;

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
    private LocalDate date;

    @Column(name="amount")
    private double amount;

    @Override
    public String toString() {
        return TransactionString.formattedTransactionString(this.date,this.amount);
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
