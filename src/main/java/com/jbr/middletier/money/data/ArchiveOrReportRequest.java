package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class ArchiveOrReportRequest {
    private long year;
    private String status;

    ArchiveOrReportRequest() {
        this.year = 1900;
        this.status = "";
    }

    ArchiveOrReportRequest(long year) {
        this.year = year;
        this.status = "";
    }

    public long getYear() { return this.year; }

    public void setYear(long year) { this.year = year; }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }
}
