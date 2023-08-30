package com.jbr.middletier.money.dto.mapper.converter;

import org.modelmapper.AbstractConverter;

import java.time.LocalDate;
import static com.jbr.middletier.money.dto.mapper.converter.LocalDateStringConverter.standardFormatter;

public class StringLocalDateConverter extends AbstractConverter<String, LocalDate> {
    @Override
    public LocalDate convert(String s) {
        if(s == null)
            return null;

        return LocalDate.parse(s, standardFormatter);
    }
}
