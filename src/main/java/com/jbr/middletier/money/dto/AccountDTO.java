package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

public class AccountDTO extends ComparableNamedDTO {
    @Pattern(regexp="^[0-9a-zA-Z]{1,45}$",message="Image Prefix can only contain letters or digits up to 45 characters.")
    private String imagePrefix;

    @Pattern(regexp="^[0-9a-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;

    private Boolean closed;

    public String getImagePrefix() {
        return imagePrefix;
    }

    public String getColour() {
        return colour;
    }

    public void setImagePrefix(String imagePrefix) {
        this.imagePrefix = imagePrefix;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    public Boolean getClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
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
