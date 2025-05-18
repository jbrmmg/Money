package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;

@Setter
@Getter
public class ArchiveOrReportRequestDTO {
    @Max(2399)
    @Min(1900)
    private int year;
    @Max(12)
    @Min(1)
    private int month;

    public ArchiveOrReportRequestDTO() {

    }

    public ArchiveOrReportRequestDTO(int year, int month) {
        this.year = year;
        this.month = month;
    }
}
