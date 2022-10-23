package com.jbr.middletier.money.util;

import com.jbr.middletier.money.data.Category;

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
}
