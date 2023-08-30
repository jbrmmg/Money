package com.jbr.middletier.money.dto;

public class AccountDTO extends ComparableNamedDTO {
    private String imagePrefix;
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
}
