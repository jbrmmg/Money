package com.jbr.middletier.money.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.text.DecimalFormat;

@Setter
@Getter
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
