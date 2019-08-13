package com.jbr.middletier.money.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="Account")
public class Account {
    @Id
    @Column(name="id")
    private String id;

    @Column(name="name")
    private String name;

    @Column(name="image_prefix")
    private String imagePrefix;

    @Column(name="colour")
    private String colour;

    @SuppressWarnings("unused")
    public Account() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImagePrefix() {
        return imagePrefix;
    }

    public String getColour() {
        return colour;
    }
}
