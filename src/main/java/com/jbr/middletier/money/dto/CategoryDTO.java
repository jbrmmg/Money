package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CategoryDTO extends ComparableNamedDTO {
    private Long sort;
    private Boolean restricted;
    @Pattern(regexp="^[\\da-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;
    private Boolean expense;
    @Pattern(regexp="^[\\da-zA-Z]{1,45}$",message="Group can only contain letters or digits up to 45 characters.")
    private String group;
    private Boolean systemUse;

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
