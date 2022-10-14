package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;

import java.time.LocalDate;

public class EmailTransaction {
    private final LocalDate date;
    private final Double amount;
    private final String description;
    private String category;
    private final String account;

    public EmailTransaction(Transaction transaction, Iterable<Category> categories) {
        this.date = transaction.getDate();
        this.amount = transaction.getAmount().getValue();
        this.description = transaction.getDescription() == null ? "" : transaction.getDescription().replace("WWW.","");
        this.account = transaction.getAccount().getId();

        for(Category nextCategory : categories) {
            if(nextCategory.getId().equalsIgnoreCase(transaction.getCategory().getId())) {
                this.category = nextCategory.getName();
                break;
            }
        }
    }

    public LocalDate getDate() {
        return date;
    }

    public Double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAccount() {
        return account;
    }

    @Override
    public String toString() {
        return description;
    }
}
