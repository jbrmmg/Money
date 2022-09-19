package com.jbr.middletier.money.dto;

public class StatusDTO {
    private String status;

    public final static StatusDTO OK = new StatusDTO("OK");

    protected StatusDTO(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
