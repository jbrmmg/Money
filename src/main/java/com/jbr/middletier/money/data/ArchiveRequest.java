package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class ArchiveRequest {
    private long year;
    private String status;

    ArchiveRequest() {
        this.year = 1900;
        this.status = "";
    }

    ArchiveRequest(long year) {
        this.year = year;
        this.status = "";
    }

    public long getYear() { return this.year; }

    public void setYear(long year) { this.year = year; }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }
}
