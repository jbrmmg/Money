package com.jbr.middletier.money.data;

public class OkStatus {
    private String status;

    public OkStatus() {
        status = "OK";
    }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }

    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
