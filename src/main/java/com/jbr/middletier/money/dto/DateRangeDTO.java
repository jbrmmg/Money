package com.jbr.middletier.money.dto;

import java.time.LocalDate;

public class DateRangeDTO {
    private String from;
    private String to;

    public DateRangeDTO() {
        this.from = null;
        this.to = null;
    }

    public DateRangeDTO(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        if(this.from == null) {
            return "1900-01-01";
        }

        return this.from;
    }

    public void setFrom(String from) { this.from = from; }

    public String getTo() {
        if(this.to == null) {
            return "2199-12-31";
        }

        return this.to;
    }

    public void setTo(String to) { this.to = to; }

    @Override
    public String toString() {
        return "[" + this.getFrom() + "->" + this.getTo() + "]";
    }
}
