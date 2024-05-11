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
        this.setMinimum(minimum);
        this.setMaximum(maximum);
    }

    public Double getMinimum() {
        if(this.minimum == null) {
            return Double.NEGATIVE_INFINITY;
        }

        return minimum;
    }

    public void setMinimum(Double minimum) {
        // Value must be positive and must be less than the maximum if provided.
        if(minimum != null && this.maximum != null && minimum > this.maximum) {
            throw new IllegalArgumentException("Value Range: Minimum cannot be greater than maximum");
        }

        this.minimum = minimum;
    }

    public Double getMaximum() {
        if(this.maximum == null) {
            return Double.POSITIVE_INFINITY;
        }

        return maximum;
    }

    public void setMaximum(Double maximum) {
        if(maximum != null && this.minimum != null && maximum < this.minimum) {
            throw new IllegalArgumentException("Value Range: Maximum cannot be less than minimum");
        }

        this.maximum = maximum;
    }

    @Override
    public String toString() {

        DecimalFormat df = new DecimalFormat("#0.00");

        return "[" +
                df.format(this.getMinimum()) +
                "->" +
                df.format(this.getMaximum()) +
                "]";
    }
}
