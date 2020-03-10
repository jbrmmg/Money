package com.jbr.middletier.money.data;

public class OkStatus {
    private String status;

    public OkStatus() {
        status = "OK";
    }

    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
