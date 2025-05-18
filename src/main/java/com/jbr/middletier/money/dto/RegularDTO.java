package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class RegularDTO {
    @Setter
    @Getter
    private Integer id;
    @Setter
    @Getter
    @Pattern(regexp="^[0-9a-zA-Z]{4}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    @Setter
    @Getter
    private BigDecimal amount;
    @Setter
    @Getter
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Setter
    @Getter
    @Pattern(regexp= "^\\d[a-zA-Z]$",message="Frequency can only contain letters or digits of 2 or 3 characters.")
    private String frequency;
    @Setter
    @Getter
    @Pattern(regexp="^[a-zA-Z]{2}$",message="Weekend Adjust can only contain 2 letters characters.")
    private String weekendAdj;
    @Setter
    @Getter
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Start must be a date in format yyyy-dd-mm")
    private String start;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Last created must be a date in format yyyy-dd-mm")
    private String lastCreated;
    @Setter
    @Getter
    @Pattern(regexp="^[\\da-zA-Z\\s]{1,40}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;

    public String getLastDate() {
        return lastCreated;
    }

    public void setLastDate(String lastCreated) {
        this.lastCreated = lastCreated;
    }
}
