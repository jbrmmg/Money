package com.jbr.middletier.money.dto;

import org.jetbrains.annotations.NotNull;

public class AccountDTO implements Comparable<AccountDTO> {
    private String id;
    private String name;
    private String imagePrefix;
    private String colour;

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

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setImagePrefix(String imagePrefix) {
        this.imagePrefix = imagePrefix;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }

    @Override
    public int compareTo(@NotNull AccountDTO o) {
        // Use the ID
        if(!this.getId().equalsIgnoreCase(o.getId())) {
            return this.getId().compareTo(o.getId());
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AccountDTO)) {
            return false;
        }

        return compareTo((AccountDTO) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public String toString() {
        return this.getId() + " [" + this.getName() + "]";
    }
}
