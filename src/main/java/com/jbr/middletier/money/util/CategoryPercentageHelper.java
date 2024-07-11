package com.jbr.middletier.money.util;

import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class CategoryPercentageHelper {
    private final Map<Category, BigDecimal> categoryMap;
    private BigDecimal total;

    public CategoryPercentageHelper(List<Transaction> transactions) {
        // Group the amounts by category
        categoryMap = new HashMap<>();

        for(Transaction nextTransaction: transactions) {
            if(Boolean.TRUE.equals(nextTransaction.getCategory().getExpense())) {
                BigDecimal amount = categoryMap.get(nextTransaction.getCategory());

                if(amount == null) {
                    amount = nextTransaction.getAmount().getValue();

                    categoryMap.put(nextTransaction.getCategory(), amount);
                } else {
                    categoryMap.put(nextTransaction.getCategory(), categoryMap.get(nextTransaction.getCategory()).add(nextTransaction.getAmount().getValue()));
                }

                total = total.add(nextTransaction.getAmount().getValue());
            }
        }
    }

    public Set<Category> getCategories() {
        // Sort the set by the category id.
        Set<Category> sortedSet = new TreeSet<>(Comparator.comparing(Category::getId));

        this.categoryMap.keySet().stream().sorted(Comparator.comparing(Category::getId)).forEach(sortedSet::add);

        return sortedSet;
    }

    public double getPercentage(Category category) {
        // If the total is greater than zero then all percentages will be zero.
        if(FinancialAmount.positive(this.total)) {
            return 0.0;
        }

        if(this.categoryMap.containsKey(category)) {
            BigDecimal amount = this.categoryMap.get(category);

            if(FinancialAmount.negative(amount)) {
                return amount.divide(this.total, RoundingMode.HALF_DOWN).doubleValue() * 100.0;
            }
        }

        return 0.0;
    }
}
