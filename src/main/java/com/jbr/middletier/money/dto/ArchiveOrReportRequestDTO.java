package com.jbr.middletier.money.dto;

public class ArchiveOrReportRequestDTO {
    private long year;
    private long month;

    public ArchiveOrReportRequestDTO() {

    }

    public ArchiveOrReportRequestDTO(long year, long month) {
        this.year = year;
        this.month = month;
    }

    public long getYear() { return this.year; }

    public void setYear(long year) { this.year = year; }

    public long getMonth() { return this.month; }

    public void setMonth(long month) { this.month = month; }
}
