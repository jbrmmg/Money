package com.jbr.middletier.money.dto;

import java.text.DecimalFormat;

public class ValueRangeDTO {
    private Double minimum;
    private Double maximum;
    private Boolean credit;

    public ValueRangeDTO() {
        this.minimum = null;
        this.maximum = null;
        this.credit = false;
    }

    public ValueRangeDTO(double minimum, double maximum, boolean credit) {
        if(minimum > maximum) {
            throw new IllegalArgumentException("Range cannot be specified with minimum greater than maximum");
        }
        this.setMinimum(minimum);
        this.setMaximum(maximum);
        this.credit = credit;
    }

    public Double getMinimum() {
        if(this.minimum == null) {
            return 0.0;
        }

        return minimum;
    }

    public void setMinimum(Double minimum) {
        // Value must be positive and must be less than the maximum if provided.
        if(minimum != null) {
            if(minimum < 0.0) {
                throw new IllegalArgumentException("Value Range: Minimum cannot be negative");
            }

            if(this.maximum != null) {
                if(minimum > this.maximum) {
                    throw new IllegalArgumentException("Value Range: Minimum cannot be greater than maximum");
                }
            }
        }

        this.minimum = minimum;
    }

    public Double getMaximum() {
        if(this.maximum == null) {
            return Double.MAX_VALUE;
        }

        return maximum;
    }

    public void setMaximum(Double maximum) {
        // Value must be positive and must be greater than the minimum if provided.
        if(maximum != null) {
            if(maximum < 0.0) {
                throw new IllegalArgumentException("Value Range: Minimum cannot be negative");
            }

            if(this.minimum != null) {
                if(maximum < this.minimum) {
                    throw new IllegalArgumentException("Value Range: Maximum cannot be less than minimum");
                }
            }
        }

        this.maximum = maximum;
    }

    public boolean getCredit() {
        if(this.credit == null) {
            return false;
        }

        return credit;
    }

    public void setCredit(Boolean credit) {
        this.credit = credit;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        DecimalFormat df = new DecimalFormat("#0.00");

        stringBuilder.append("[");
        stringBuilder.append(this.getCredit() ? "CR" : "DB");
        stringBuilder.append(" ");
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
