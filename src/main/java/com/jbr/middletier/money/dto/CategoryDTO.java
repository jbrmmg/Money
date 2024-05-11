package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

public class CategoryDTO extends ComparableNamedDTO {
    private Long sort;
    private Boolean restricted;
    @Pattern(regexp="^[\\da-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;
    private Boolean expense;
    @Pattern(regexp="^[\\da-zA-Z]{1,45}$",message="Group can only contain letters or digits up to 45 characters.")
    private String group;
    private Boolean systemUse;

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

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
