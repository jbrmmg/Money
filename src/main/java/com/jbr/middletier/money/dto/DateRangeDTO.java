package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.data.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateRangeDTO {
    private final LocalDate from;
    private final LocalDate to;

    private LocalDate getDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Transaction.TRANSACTION_DATE_FORMAT);

        if(dateString == null) {
            return null;
        }

        return LocalDate.parse(dateString,formatter);
    }

    public DateRangeDTO(String from, String to) {
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
