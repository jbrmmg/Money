package com.jbr.middletier.money.data;

public class StatusResponse {
    public String status;
    public String message;

    public StatusResponse() {
        status = "OK";
        message = "";
    }

    public StatusResponse(String errorMsg) {
        status = "FAILURE";
        message = errorMsg;
    }
}
