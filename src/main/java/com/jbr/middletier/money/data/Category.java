package com.jbr.middletier.money.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by jason on 04/03/17.
 */
@Entity
@Table(name="Category")
public class Category {
    @Id
    @Column(name="id")
    private String id;

    @Column(name="name")
    private String name;

    @Column(name="sort")
    private Long sort;

    @Column(name="restricted")
    private String restricted;

    @Column(name="colour")
    private String colour;

    @Column(name="groupid")
    private String group;

    @Column(name="systemUse")
    private String systemUse;

    protected  Category() {}

    public String getId() {
        return this.id;
    }

    public String getSource() {
        return this.name;
    }

    public Long getSort() {
        return this.sort;
    }

    public String getRestricted() {
        return this.restricted;
    }

    public String getColour() {
        return this.colour;
    }

    public String getGroup() {
        return this.group;
    }

    public String getSystemUse() {
        return this.systemUse;
    }

    public String getName() { return this.name; }
}
