package com.jbr.middletier.money.dto;

public class ArchiveOrReportRequestDTO {
    private int year;
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
