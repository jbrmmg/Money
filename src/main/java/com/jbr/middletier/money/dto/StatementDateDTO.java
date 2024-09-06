package com.jbr.middletier.money.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.text.DecimalFormat;

public class StatementDateDTO {
    @Min(1)
    @Max(9999)
    private Integer year;
    @Min(1)
    @Max(12)
    private Integer month;
    private boolean none;

    public StatementDateDTO() {
        this.year = null;
        this.month = null;
        this.none = false;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public boolean isNone() {
        return none;
    }

    public void setNone(boolean none) {
        this.none = none;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("(");
        if(this.year != null) {
            DecimalFormat df = new DecimalFormat("0000");
            builder.append(df.format(this.year));
        } else {
            builder.append("any");
        }
        builder.append("-");
        if(this.month != null) {
            DecimalFormat df = new DecimalFormat("00");
            builder.append(df.format(this.month));
        } else {
            builder.append("any");
        }
        builder.append(")");

        return builder.toString();
    }
}
