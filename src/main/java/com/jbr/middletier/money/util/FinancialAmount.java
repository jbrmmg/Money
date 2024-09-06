package com.jbr.middletier.money.util;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;

@JsonSerialize(using = FinancialAmountSerializer.class)
@JsonDeserialize(using = FinancialAmountDeserializer.class)
public class FinancialAmount implements Comparable<FinancialAmount>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private BigDecimal value;

    public FinancialAmount(BigDecimal value) {
        this.value = value;
    }

    public FinancialAmount() {
        this.value = BigDecimal.ZERO;
    }

    public BigDecimal getValue() {
        return this.value;
    }

    public void increment(FinancialAmount addition) {
        this.value = this.value.add(addition.getValue());
    }

    public void increment(BigDecimal addition) {
        this.value = this.value.add(addition);
    }

    public void decrement(FinancialAmount subtraction) {
        increment(FinancialAmount.flipSign(subtraction));
    }

    public FinancialAmountType getType() {
        if(this.value.compareTo(BigDecimal.ZERO) > 0) {
            return FinancialAmountType.CR;
        }

        return FinancialAmountType.DB;
    }

    public void setType(FinancialAmountType type) {
        if( (type == FinancialAmountType.CR && this.value.compareTo(BigDecimal.ZERO) < 0) || (type == FinancialAmountType.DB && this.value.compareTo(BigDecimal.ZERO) > 0)) {
            this.value = FinancialAmount.flipSign(this.value);
        }
    }

    public boolean isGreaterThan(FinancialAmount rhs) {
        return this.value.compareTo(rhs.value) > 0;
    }

    public boolean isLessThan(FinancialAmount rhs) {
        return this.value.compareTo(rhs.value) < 0;
    }

    public static String internalToString(BigDecimal value) {
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        return decimalFormat.format(value);
    }

    public static String internalToStringNoComma(BigDecimal value) {
        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        return decimalFormat.format(value);
    }

    public static String internalToStringNoComma(FinancialAmount value) {
        return internalToStringNoComma(value.getValue());
    }

    public static boolean positive(FinancialAmount value) {
        return positive(value.getValue());
    }

    public static boolean positive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean negative(FinancialAmount value) {
        return negative(value.getValue());
    }

    public static boolean negative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    public static BigDecimal flipSign(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(-1));
    }

    public static BigDecimal flipSign(FinancialAmount value) {
        return flipSign(value.getValue());
    }

    public static boolean zero(FinancialAmount value) {
        return zero(value.getValue());
    }

    public static boolean zero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public String toAbsString() {
        return internalToString(this.value.abs());
    }

    public String toFormattedString(int size) {
        String fieldSizeFormat = String.format("%%%ds", size);
        String number = String.format(fieldSizeFormat,toAbsString());
        return String.format("%s %-2s",number,this.getType().toString());
    }

    @Override
    public String toString() {
        return internalToString(this.value);
    }

    @Override
    public int compareTo(@NotNull FinancialAmount financialAmount) {
        BigDecimal internalValue = this.value;
        return internalValue.compareTo(financialAmount.value);
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof FinancialAmount financialAmount) {
            BigDecimal internalValue = this.value;
            return internalValue.equals(financialAmount.value);
        }

        if(o instanceof BigDecimal doubleAmount) {
            BigDecimal internalValue = this.value;
            return internalValue.equals(doubleAmount);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }
}
