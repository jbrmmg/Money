package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

public class StatusDTO {
    @Pattern(regexp="^[0-9a-zA-Z\\s]{1,40}$",message="Status can only contain letters or digits up to 45 characters.")
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
