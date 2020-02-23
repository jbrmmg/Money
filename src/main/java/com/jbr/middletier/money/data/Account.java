package com.jbr.middletier.money.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="Account")
public class Account {
    @Id
    @Size(max=4)
    @Column(name="id")
    @NotNull(message = "Account ID cannot be null.")
    private String id;

    @Size(max=45)
    @Column(name="name")
    private String name;

    @Size(max=45)
    @Column(name="image_prefix")
    private String imagePrefix;

    @Size(max=6)
    @Column(name="colour")
    private String colour;

    @SuppressWarnings("unused")
    public Account() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) { this.name = name; }

    public String getImagePrefix() {
        return imagePrefix;
    }

    public void setImagePrefix(String imagePrefix) { this.imagePrefix = imagePrefix; }

    public String getColour() {
        return colour;
    }

    public void setColor(String colour) { this.colour = colour; }
}
