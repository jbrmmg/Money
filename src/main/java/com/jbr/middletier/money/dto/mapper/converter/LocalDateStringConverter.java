package com.jbr.middletier.money.dto.mapper.converter;

import org.modelmapper.AbstractConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class LocalDateStringConverter extends AbstractConverter<LocalDate,String> {
    private static final String STANDARD_DATE_FORMAT = "uuuu-MM-dd";

    public static final DateTimeFormatter standardFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(STANDARD_DATE_FORMAT)
            .toFormatter(Locale.ENGLISH);

    @Override
    public String convert(LocalDate localDate) {
        if(localDate == null)
            return null;

        return localDate.format(standardFormatter);
    }
}
