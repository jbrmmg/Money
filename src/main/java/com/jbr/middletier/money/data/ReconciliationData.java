package com.jbr.middletier.money.data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    private boolean compareDateWithoutTime(Date leftSide, Date rightSide) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        String leftDateString = sdf.format(leftSide);
        String rightDateString = sdf.format(rightSide);

        return leftDateString.equalsIgnoreCase(rightDateString);
    }

    private long dateDifferenceWithoutTime(Date leftSide, Date rightSide) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            String leftDateString = sdf.format(leftSide);
            String rightDateString = sdf.format(rightSide);

            Date newLeftSide = sdf.parse(leftDateString);
            Date newRightSide = sdf.parse(rightDateString);

            return Math.abs(newLeftSide.getTime() - newRightSide.getTime());
        } catch(Exception e) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof Transaction)) {
            return false;
        }

        Transaction transaction = (Transaction) o;

        return transaction.getAmount() == this.amount && compareDateWithoutTime(transaction.getDate(),this.date);
    }

    public long closeMatch(Transaction transaction) {
        if(transaction.getAmount() != this.amount) {
            return -1;
        }

        // Subtract one date from the other.
        return TimeUnit.DAYS.convert(dateDifferenceWithoutTime(this.date,transaction.getDate()),TimeUnit.MILLISECONDS);
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

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDescription() { return this.description; }
}
