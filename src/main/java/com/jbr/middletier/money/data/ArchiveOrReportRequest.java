package com.jbr.middletier.money.data;

public class ArchiveOrReportRequest {
    private long year;
    private long month;

    public ArchiveOrReportRequest() {

    }

    public ArchiveOrReportRequest(long year, long month) {
        this.year = year;
        this.month = month;
    }

    public long getYear() { return this.year; }

    public void setYear(long year) { this.year = year; }

    public long getMonth() { return this.month; }

    public void setMonth(long month) { this.month = month; }
}
