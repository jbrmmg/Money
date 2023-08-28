package com.jbr.middletier.money.dto.mapper.converter;

import org.modelmapper.AbstractConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class LocalDateStringConverter extends AbstractConverter<LocalDate,String> {
    @Override
    protected String convert(LocalDate localDate) {
        if(localDate == null) return null;

        return localDate.format(new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("uuuu-MMM-dd")
                .toFormatter(Locale.ENGLISH));
    }
}
