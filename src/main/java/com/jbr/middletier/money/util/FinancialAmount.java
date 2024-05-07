package com.jbr.middletier.money.util;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.text.DecimalFormat;

public class FinancialAmount {
    private double value;

    public FinancialAmount(double value) {
        this.value = value;
    }

    public FinancialAmount() { this.value = 0.0; }

    public double getValue() {
        return this.value;
    }

    public void increment(FinancialAmount addition) {
        this.value += addition.getValue();
    }

    public void increment(double addition) { this.value += addition; }

    public void decrement(FinancialAmount subtraction) {
        increment(-1 * subtraction.value);
    }

    public FinancialAmountType getType() {
        if(this.value < 0) {
            return FinancialAmountType.DB;
        }

        if(this.value > 0) {
            return FinancialAmountType.CR;
        }

        return FinancialAmountType.Z;
    }

    public void setType(FinancialAmountType type) {
        if( (type == FinancialAmountType.CR && this.value < 0) || (type == FinancialAmountType.DB && this.value > 0)) {
            this.value = this.value * -1;
        }
    }

    public boolean isGreaterThan(FinancialAmount rhs) {
        return this.value > rhs.value;
    }

    public boolean isLessThan(FinancialAmount rhs) {
        return this.value < rhs.value;
    }

    public static String internalToString(double value) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        return decimalFormat.format(value);
    }

    public String toAbsString() {
        return internalToString(Math.abs(this.value));
    }

    @Override
    public String toString() {
        return internalToString(this.value);
    }
}
