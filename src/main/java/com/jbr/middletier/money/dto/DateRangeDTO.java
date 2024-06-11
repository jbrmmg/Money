package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.config.Constants;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateRangeDTO {
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String from;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "From must be a date in format yyyy-dd-mm")
    private String to;

    public DateRangeDTO(String from, String to) {
        if(from != null) {
            this.setFrom(from);
        }
        if(to != null) {
            this.setTo(to);
        }
    }

    private LocalDate convertToLocalDate(String date) {
        try {
            return LocalDate.parse(date, Constants.MONEY_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        // String must be in the format yyyy-MM-dd and if provided then must be before the 'to' date.
        LocalDate date = convertToLocalDate(from);
        if(date == null) {
            throw new IllegalArgumentException("Date Range: the dates must be in the format " + Constants.MONEY_DATE_FORMAT);
        }

        if(this.getTo() != null) {
            LocalDate toDate = convertToLocalDate(this.getTo());
            if(date.isAfter(toDate)) {
                throw new IllegalArgumentException("Date Range: the from date MUST be before the to date.");
            }
        }

        this.from = from;
    }

    public String getTo() {
        return this.to;
    }

    public void setTo(String to) {
        // String must be in the format yyyy-MM-dd and if provided then must be after the 'from' date.
        LocalDate date = convertToLocalDate(to);
        if(date == null) {
            throw new IllegalArgumentException("Date Range: the dates must be in the format " + Constants.MONEY_DATE_FORMAT);
        }

        if(this.getFrom() != null) {
            LocalDate fromDate = convertToLocalDate(this.getFrom());
            if(date.isBefore(fromDate)) {
                throw new IllegalArgumentException("Date Range: the from date MUST be after the from date.");
            }
        }

        this.to = to;
    }

    @Override
    public String toString() {
        return "[" + this.getFrom() + "->" + this.getTo() + "]";
    }
}
