package com.jbr.middletier.money.dto;

import java.util.Date;

public class RegularDTO {
    private Integer id;
    private AccountDTO account;
    private double amount;
    private CategoryDTO category;
    private String frequency;
    private String weekendAdj;
    private Date start;
    private Date lastCreated;
    private String description;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public CategoryDTO getCategory() {
        return category;
    }

    public void setCategory(CategoryDTO category) {
        this.category = category;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getWeekendAdj() {
        return weekendAdj;
    }

    public void setWeekendAdj(String weekendAdj) {
        this.weekendAdj = weekendAdj;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getLastDate() {
        return lastCreated;
    }

    public void setLastDate(Date lastCreated) {
        this.lastCreated = lastCreated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
