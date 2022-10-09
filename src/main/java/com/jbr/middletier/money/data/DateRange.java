package com.jbr.middletier.money.data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateRange {
    private LocalDate from;
    private LocalDate to;

    private LocalDate getDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Transaction.TRANSACTION_DATE_FORMAT);

        if(dateString == null) {
            return null;
        }

        return LocalDate.parse(dateString,formatter);
    }

    public DateRange(String from, String to) {
        this.from = getDate(from);
        this.to = getDate(to);
    }

    public LocalDate getFrom() {
        return this.from;
    }

    public LocalDate getTo() {
        return this.to;
    }
}
