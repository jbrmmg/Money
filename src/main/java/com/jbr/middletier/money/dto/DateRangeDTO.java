package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.dto.mapper.UtilityMapper;

import java.time.LocalDate;

public class DateRangeDTO {
    private final LocalDate from;
    private final LocalDate to;

    public DateRangeDTO(UtilityMapper mapper, String from, String to) {
        this.from = ( from == null ) ? null : mapper.map(from,LocalDate.class);
        this.to = ( to == null ) ? null : mapper.map(to,LocalDate.class);
    }

    public LocalDate getFrom() {
        return this.from;
    }

    public LocalDate getTo() {
        return this.to;
    }
}
