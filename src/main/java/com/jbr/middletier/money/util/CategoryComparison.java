package com.jbr.middletier.money.util;

import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class CategoryComparison {
    private final Category category;
    private final FinancialAmount thisMonth;
    private final FinancialAmount previousMonth;

    public CategoryComparison(Category category) {
        this.category = category;
        this.thisMonth = new FinancialAmount(BigDecimal.ZERO);
        this.previousMonth = new FinancialAmount(BigDecimal.ZERO);
    }

    public void incrementThisMonth(BigDecimal increment) {
        this.thisMonth.increment(increment);
    }

    public void incrementPreviousMonth(BigDecimal increment) {
        this.previousMonth.increment(increment);
    }

    public double getPercentageChange() {
        return this.thisMonth.getValue().subtract(this.previousMonth.getValue()).divide(this.previousMonth.getValue(), RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
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
