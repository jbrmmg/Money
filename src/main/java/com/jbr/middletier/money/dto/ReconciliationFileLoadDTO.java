package com.jbr.middletier.money.dto;

import javax.validation.constraints.Pattern;

public class ReconciliationFileLoadDTO {
    @Pattern(regexp="^[\\da-zA-Z._]{1,100}$",message="Filename must be alphanumeric (plus . or _) upto 200 characters")
    private String filename;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String toString() {
        return filename;
    }
}
