package com.jbr.middletier.money.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Constants {
    public static final String MONEY_DATE_FORMAT = "yyyy-MM-dd";
    public static final DateTimeFormatter MONEY_DATE_FORMATTER = DateTimeFormatter.ofPattern(MONEY_DATE_FORMAT);

    public static final String MONEY_EARLIEST_DATE_STRING = "1900-01-01";
    public static final LocalDate MONEY_EARLIEST_DATE = LocalDate.parse(MONEY_EARLIEST_DATE_STRING,MONEY_DATE_FORMATTER);

    public static final String MONEY_LATEST_DATE_STRING = "2199-12-31";
    public static final LocalDate MONEY_LATEST_DATE = LocalDate.parse(MONEY_LATEST_DATE_STRING,MONEY_DATE_FORMATTER);

    public static final String COLOUR_WHITE = "FFFFFF";
    public static final String COLOUR_RED = "FF0000";

    private Constants() {
        // Prevent instantiation.
    }
}
