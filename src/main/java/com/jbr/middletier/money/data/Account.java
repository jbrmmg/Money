package com.jbr.middletier.money.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="Account")
public class Account implements Serializable {
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

    @Column(name="closed")
    private Boolean closed;

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

    public void setColour(String colour) { this.colour = colour; }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }
}
