package com.jbr.middletier.money.dto;

public class CategoryDTO {
    private String id;
    private String name;
    private Long sort;
    private Boolean restricted;
    private String colour;
    private Boolean expense;
    private String group;
    private Boolean systemUse;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSort() {
        return sort;
    }

    public void setSort(Long sort) {
        this.sort = sort;
    }

    public Boolean getRestricted() {
        return restricted;
    }

    public void setRestricted(Boolean restricted) {
        this.restricted = restricted;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Boolean getExpense() {
        return expense;
    }

    public void setExpense(Boolean expense) {
        this.expense = expense;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Boolean getSystemUse() {
        return systemUse;
    }

    public void setSystemUse(Boolean systemUse) {
        this.systemUse = systemUse;
    }
}
