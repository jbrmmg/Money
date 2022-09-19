package com.jbr.middletier.money.dto;

public class StatusDTO {
    private String status;

    public static final StatusDTO OK = new StatusDTO("OK");

    protected StatusDTO(String status) {
        this.status = status;
    }

    public StatusDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
