package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

public class RegularDTO {
    private Integer id;
    @Pattern(regexp="^[0-9a-zA-Z]{4}$",message="Account can only contain letters or digits of 4 characters.")
    private String accountId;
    private double amount;
    @Pattern(regexp="^[0-9a-zA-Z]{3}$",message="Category can only contain letters or digits of 3 characters.")
    private String categoryId;
    @Pattern(regexp= "^\\d[a-zA-Z]$",message="Frequency can only contain letters or digits of 2 or 3 characters.")
    private String frequency;
    @Pattern(regexp="^[a-zA-Z]{2}$",message="Weekend Adjust can only contain 2 letters characters.")
    private String weekendAdj;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Start must be a date in format yyyy-dd-mm")
    private String start;
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Last created must be a date in format yyyy-dd-mm")
    private String lastCreated;
    @Pattern(regexp="^[\\da-zA-Z\\s]{1,40}$",message="Description can only contain letters or digits up to 45 characters.")
    private String description;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getWeekendAdj() {
        return this.weekendAdj;
    }

    public void setWeekendAdj(String weekendAdj) {
        this.weekendAdj = weekendAdj;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getLastDate() {
        return lastCreated;
    }

    public void setLastDate(String lastCreated) {
        this.lastCreated = lastCreated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
