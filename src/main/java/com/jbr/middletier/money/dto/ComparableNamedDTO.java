package com.jbr.middletier.money.dto;

import org.jetbrains.annotations.NotNull;

public class ComparableNamedDTO implements Comparable<ComparableNamedDTO> {
    private String id;
    private String name;

    protected ComparableNamedDTO() {
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int compareTo(@NotNull ComparableNamedDTO o) {
        // Use the ID
        if(!this.getId().equalsIgnoreCase(o.getId())) {
            return this.getId().compareTo(o.getId());
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ComparableNamedDTO)) {
            return false;
        }

        return compareTo((ComparableNamedDTO) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.getId().toUpperCase().hashCode();
    }

    @Override
    public String toString() {
        return this.getId() + " [" + this.getName() + "]";
    }
}
