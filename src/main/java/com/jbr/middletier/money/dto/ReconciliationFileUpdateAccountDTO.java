package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReconciliationFileUpdateAccountDTO {
    @Pattern(regexp="^[\\da-zA-Z._]{1,100}$",message="Filename must be alphanumeric (plus . or _) upto 200 characters")
    private String filename;
    @Pattern(regexp="^[\\da-zA-Z]{1,4}$",message="ID can only contain letters or digits up to 4 characters.")
    private String accountId;

    @Override
    public String toString() {
        return filename + " to " + accountId;
    }
}
