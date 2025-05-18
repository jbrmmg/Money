package com.jbr.middletier.money.util;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class DateRange {
    private LocalDate from;
    private LocalDate to;

}
