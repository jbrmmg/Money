package com.jbr.middletier.money.util;

import java.time.LocalDate;

public class DateRange {
    private LocalDate from;
    private LocalDate to;

    public LocalDate getFrom() {
        return this.from;
    }

    public void setFrom(LocalDate from) { this.from = from; }

    public LocalDate getTo() {
        return this.to;
    }

    public void setTo(LocalDate to) { this.to = to; }
}
