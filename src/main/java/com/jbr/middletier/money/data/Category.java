package com.jbr.middletier.money.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by jason on 04/03/17.
 */
@SuppressWarnings("unused")
@Entity
@Table(name="Category")
public class Category {
    @Id
    @NotNull
    @Size(max=3)
    @Column(name="id")
    private String id;

    @Size(max=45)
    @Column(name="name")
    private String name;

    @Size(max=1000000)
    @Column(name="sort")
    private Long sort;

    @NotNull
    @Column(name="restricted")
    private Boolean restricted;

    @Size(max=6)
    @Column(name="colour")
    private String colour;

    @Size(max=6)
    @Column(name="expense")
    private Boolean expense;

    @Size(max=45)
    @Column(name="groupid")
    private String group;

    @NotNull
    @Column(name="systemUse")
    private Boolean systemUse;

    public  Category() {}

    public String getId() {
        return this.id;
    }

    public void setId(String id) { this.id = id; }

    public String getSource() {
        return this.name;
    }

    public Long getSort() {
        return this.sort;
    }

    public void setSort(Long sort) { this.sort = sort; }

    public Boolean getRestricted() {
        return this.restricted;
    }

    public void setRestricted(Boolean restricted) { this.restricted = restricted; }

    public String getColour() {
        return this.colour;
    }

    public void setColour(String colour) { this.colour = colour; }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) { this.group = group; }

    public Boolean getSystemUse() {
        return this.systemUse;
    }

    public void setSystemUse(Boolean systemUse) { this.systemUse = systemUse; }

    public Boolean getExpense() { return this.expense; }

    public void setExpense(Boolean expense) { this.expense = expense; }

    public String getName() { return this.name; }

    public void setName(String name) { this.name = name; }
}
