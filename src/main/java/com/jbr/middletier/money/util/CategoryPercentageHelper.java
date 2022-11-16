package com.jbr.middletier.money.util;

import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;

import java.util.*;

public class CategoryPercentageHelper {
    private final Map<Category,Double> categoryMap;
    private double total;

    public CategoryPercentageHelper(List<Transaction> transactions) {
        // Group the amounts by category
        categoryMap = new HashMap<>();

        for(Transaction nextTransaction: transactions) {
            if(Boolean.TRUE.equals(nextTransaction.getCategory().getExpense())) {
                Double amount = categoryMap.get(nextTransaction.getCategory());

                if(amount == null) {
                    amount = nextTransaction.getAmount().getValue();

                    categoryMap.put(nextTransaction.getCategory(), amount);
                } else {
                    categoryMap.put(nextTransaction.getCategory(), categoryMap.get(nextTransaction.getCategory()) + nextTransaction.getAmount().getValue());
                }

                total += nextTransaction.getAmount().getValue();
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
        if(this.total >= 0.0) {
            return 0.0;
        }

        if(this.categoryMap.containsKey(category)) {
            Double amount = this.categoryMap.get(category);

            if(amount < 0.0) {
                return amount / this.total * 100.0;
            }
        }

        return 0.0;
    }
}
