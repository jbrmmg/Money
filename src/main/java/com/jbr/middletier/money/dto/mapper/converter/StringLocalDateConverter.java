package com.jbr.middletier.money.dto.mapper.converter;

import org.modelmapper.AbstractConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class StringLocalDateConverter extends AbstractConverter<String, LocalDate> {
    @Override
    protected LocalDate convert(String s) {
        if(s == null) return null;

        return LocalDate.parse(s, new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("uuuu-MMM-dd")
                .toFormatter(Locale.ENGLISH));
    }
}
