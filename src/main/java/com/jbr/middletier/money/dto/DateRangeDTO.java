package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.dto.mapper.DtoComplexModelMapper;
import java.time.LocalDate;


public class DateRangeDTO {
    private final LocalDate from;
    private final LocalDate to;

    public DateRangeDTO(String from, String to) {
        this.from = DtoComplexModelMapper.stringLocalDateConverter.convert(from);
        this.to = DtoComplexModelMapper.stringLocalDateConverter.convert(to);
    }

    public LocalDate getFrom() {
        return this.from;
    }

    public LocalDate getTo() {
        return this.to;
    }
}
