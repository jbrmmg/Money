package com.jbr.middletier.money.util;

import java.text.DecimalFormat;

public class FinancialAmount {
    private double value;

    public FinancialAmount(double value) {
        this.value = value;
    }

    public double getValue() {
        return this.value;
    }

    public void increment(FinancialAmount addition) {
        this.value += addition.getValue();
    }

    @Override
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("#.00");
        return decimalFormat.format(this.value);
    }
}
