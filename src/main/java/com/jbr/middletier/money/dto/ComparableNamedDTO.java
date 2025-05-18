package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Setter
@Getter
public class ComparableNamedDTO implements Comparable<ComparableNamedDTO> {
    @Pattern(regexp="^[\\da-zA-Z]{1,4}$",message="ID can only contain letters or digits up to 4 characters.")
    private String id;
    @Pattern(regexp="^[\\da-zA-Z\\s]{1,45}$",message="Name can only contain letters or digits up to 45 characters.")
    private String name;

    protected ComparableNamedDTO() {
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
