package com.jbr.middletier.money.dto;

import java.text.DecimalFormat;

public class ValueRangeDTO {
    private Double minimum;
    private Double maximum;

    public ValueRangeDTO() {
        this.minimum = null;
        this.maximum = null;
    }

    public ValueRangeDTO(double minimum, double maximum) {
        if(minimum > maximum) {
            throw new IllegalArgumentException("Range cannot be specified with minimum greater than maximum");
        }
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public Double getMinimum() {
        if(this.minimum == null) {
            return 0.0;
        }

        return minimum;
    }

    public void setMinimum(Double minimum) {
        this.minimum = minimum;
    }

    public Double getMaximum() {
        if(this.maximum == null) {
            return Double.MAX_VALUE;
        }

        return maximum;
    }

    public void setMaximum(Double maximum) {
        this.maximum = maximum;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        DecimalFormat df = new DecimalFormat("#0.00");

        stringBuilder.append("[");
        stringBuilder.append(df.format(this.getMinimum()));
        stringBuilder.append("->");
        if(this.maximum == null) {
            stringBuilder.append("max");
        } else {
            stringBuilder.append(df.format(this.getMaximum()));
        }
        stringBuilder.append("]");

        return stringBuilder.toString();
    }
}
