package com.jbr.middletier.money.dto;


import com.jbr.middletier.money.schedule.AdjustmentType;

import java.time.LocalDate;

public class RegularDTO {
    private Integer id;
    private AccountDTO account;
    private double amount;
    private CategoryDTO category;
    private String frequency;
    private AdjustmentType weekendAdj;
    private LocalDate start;
    private LocalDate lastCreated;
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

    public AdjustmentType getWeekendAdj() {
        return this.weekendAdj;
    }

    public void setWeekendAdj(AdjustmentType weekendAdj) {
        this.weekendAdj = weekendAdj;
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public LocalDate getLastDate() {
        return lastCreated;
    }

    public void setLastDate(LocalDate lastCreated) {
        this.lastCreated = lastCreated;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
