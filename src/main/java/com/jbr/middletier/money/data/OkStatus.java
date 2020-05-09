package com.jbr.middletier.money.data;

@SuppressWarnings("unused")
public class OkStatus {
    private String status;

    private OkStatus() {
        status = "OK";
    }

    public String getStatus() { return this.status; }

    public void setStatus(String status) { this.status = status; }

    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
