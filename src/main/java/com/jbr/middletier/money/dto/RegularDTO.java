package com.jbr.middletier.money.dto;

public class RegularDTO {
    private Integer id;
    private String accountId;
    private double amount;
    private String categoryId;
    private String frequency;
    private String weekendAdj;
    private String start;
    private String lastCreated;
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
