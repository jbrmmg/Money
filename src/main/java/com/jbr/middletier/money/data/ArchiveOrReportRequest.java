package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class ArchiveOrReportRequest {
    private long year;
    private long month;
    private String status;

    public ArchiveOrReportRequest() {
        this.year = 1900;
        this.month = 1;
        this.status = "";
    }

    ArchiveOrReportRequest(long year, long month) {
        this.year = year;
        this.month = month;
        this.status = "";
    }

    public long getYear() { return this.year; }

    public void setYear(long year) { this.year = year; }

    public long getMonth() { return this.month; }

    public void setMonth(long month) { this.month = month; }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }
}
