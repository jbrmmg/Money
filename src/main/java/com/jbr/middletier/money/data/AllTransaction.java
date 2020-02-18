package com.jbr.middletier.money.data;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jason on 09/03/17.
 */
@Entity
@Table(name="all_transaction")
public class AllTransaction {
    @Id
    @Column(name="id")
    private int id;

    @Column(name="account_id")
    private String account;

    @Column(name="category_id")
    private String category;

    @Column(name="date")
    private Date date;

    @Column(name="amount")
    private double amount;

    @Column(name="statement_id")
    private String statement;

    @Column(name="opposite_id")
    private Integer oppositeiId;

    @Column(name="locked")
    private String locked;

    @Column(name="category_name")
    private String categoryName;

    @Column(name="colour")
    private String colour;

    @Column(name="category_group")
    private String group;

    @Column(name="opp_statement_id")
    private String oppositeStatementId;

    @Column(name="description")
    private String description;

    public AllTransaction() {
    }

    public int getId() { return this.id; }

    public String getAccount() { return this.account; }

    public String getCategory() { return this.categoryName; }

    public double getAmount() { return this.amount; }

    public String getCatColour() { return this.colour; }

    public String getCategoryId() { return this.category; }

    public String getGroup() { return this.group; }

    private String getStatementId() { return this.statement; }

    public String getOppositeStatementId() { return this.oppositeStatementId; }

    public String getDescription() { return this.description; }

    public int getOppositeId() { return (oppositeiId != null) ? oppositeiId : -1; }

    public Date getDate() {
        return this.date;
    }

    public int getDateYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.date);
        return calendar.get(Calendar.YEAR);
    }

    public String getDateMonth() {
        return new SimpleDateFormat("MMM").format(this.date);
    }

    public int getDateDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public boolean getLocked() { return this.locked.equalsIgnoreCase("Y"); }

    public boolean getReconciled() {
        if (getStatementId() != null) {
            if (getStatementId().length() > 0) {
                return true;
            }
        }

        return false;
    }
}
