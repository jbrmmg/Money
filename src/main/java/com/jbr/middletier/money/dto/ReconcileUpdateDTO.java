package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReconcileUpdateDTO {
    private int id;
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Category can only contain letters of 3 characters.")
    private String categoryId;
    @Pattern(regexp="^[\\da-zA-Z]{3}$",message="Type can only contain letters of 3 characters.")
    private String type;

    public ReconcileUpdateDTO() {
        this.id = -1;
        this.categoryId = "";
        this.type = "rec";
    }

}
