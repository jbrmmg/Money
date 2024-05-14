package com.jbr.middletier.money.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * Created by jason on 07/03/17.
 */

@Entity
@Table(name="Account")
public class Account implements Serializable {
    @Id
    @Column(name="id")
    @NotNull(message = "Account ID cannot be null.")
    @Pattern(regexp="^[0-9A-Z]{4}$",message="Account id must be uppercase and/or numbers of length 4.")
    private String id;

    @Column(name="name")
    @Pattern(regexp="^[0-9A-Za-z\\s]{1,45}$",message="Account name must be alpha numeric upto 45 characters.")
    private String name;

    @Column(name="image_prefix")
    @Pattern(regexp="^[0-9a-zA-Z]{1,45}$",message="Image Prefix can only contain letters or digits up to 45 characters.")
    private String imagePrefix;

    @Column(name="colour")
    @Pattern(regexp="^[0-9a-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
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
