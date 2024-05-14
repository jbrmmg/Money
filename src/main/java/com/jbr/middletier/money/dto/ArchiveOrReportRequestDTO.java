package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Max;

import javax.validation.constraints.Min;

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

    public int getYear() { return this.year; }

    public void setYear(int year) { this.year = year; }

    public int getMonth() { return this.month; }

    public void setMonth(int month) { this.month = month; }
}
