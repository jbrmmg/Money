package com.jbr.middletier.money.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;

@Setter
@Getter
public class ReconciliationFileLoadDTO {
    @Pattern(regexp="^[\\da-zA-Z._]{1,100}$",message="Filename must be alphanumeric (plus . or _) upto 200 characters")
    private String filename;

    @Override
    public String toString() {
        return filename;
    }
}
