package com.jbr.middletier.money.data;

import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransactionString;
import org.hibernate.annotations.*;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;

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
    private LocalDate date;

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

    public Transaction (Account account, Category category, LocalDate date, double amount, String description) {
        this.account = account;
        this.category = category;
        this.date = date;
        this.amount = amount;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public Account getAccount() {
        return this.account;
    }

    public FinancialAmount getAmount() { return new FinancialAmount(this.amount); }

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

    public LocalDate getDate() { return this.date; }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof Transaction transaction)) {
            return false;
        }

        return this.toString().equals(transaction.toString());
    }

    @Override
    public String toString() {
        return TransactionString.formattedTransactionString(this.date,this.amount);
    }
}
