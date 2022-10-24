package com.jbr.middletier.money.util;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryComparison {
    private final Category category;
    private final FinancialAmount thisMonth;
    private final FinancialAmount previousMonth;

    public CategoryComparison(Category category) {
        this.category = category;
        this.thisMonth = new FinancialAmount(0);
        this.previousMonth = new FinancialAmount(0);
    }

    public Category getCategory() {
        return this.category;
    }

    public FinancialAmount getThisMonth() {
        return this.thisMonth;
    }

    public FinancialAmount getPreviousMonth() {
        return this.previousMonth;
    }

    public void incrementThisMonth(double increment) {
        this.thisMonth.increment(increment);
    }

    public void incrementPreviousMonth(double increment) {
        this.previousMonth.increment(increment);
    }

    public double getPercentageChange() {
        return ( ( this.thisMonth.getValue() - this.previousMonth.getValue() ) / this.previousMonth.getValue() ) * 100.0;
    }

    public static Map<String, CategoryComparison> categoryCompare(List<Transaction> transactions, List<Transaction> previousTransactions) {
        Map<String, CategoryComparison> result = new HashMap<>();

        for(Transaction nextTransaction: transactions) {
            // Has this category already been seen?
            CategoryComparison categoryComparison;
            if(result.containsKey(nextTransaction.getCategory().getId())) {
                categoryComparison = result.get(nextTransaction.getCategory().getId());
            } else {
                categoryComparison = new CategoryComparison(nextTransaction.getCategory());
                result.put(nextTransaction.getCategory().getId(),categoryComparison);
            }

            // Update the details on the category.
            categoryComparison.incrementThisMonth(nextTransaction.getAmount().getValue());
        }

        for(Transaction nextTransaction: previousTransactions) {
            // Has this category already been seen?
            CategoryComparison categoryComparison;
            if(result.containsKey(nextTransaction.getCategory().getId())) {
                categoryComparison = result.get(nextTransaction.getCategory().getId());
            } else {
                categoryComparison = new CategoryComparison(nextTransaction.getCategory());
                result.put(nextTransaction.getCategory().getId(),categoryComparison);
            }

            // Update the details on the category.
            categoryComparison.incrementPreviousMonth(nextTransaction.getAmount().getValue());
        }

        return result;
    }
}
