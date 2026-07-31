package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.schedule.AdjustmentType;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AccountDTO extends ComparableNamedDTO {
    @Pattern(regexp="^[\\da-zA-Z]{1,45}$",message="Image Prefix can only contain letters or digits up to 45 characters.")
    private String imagePrefix;

    @Pattern(regexp="^[\\da-fA-F]{6}$",message="Colour must be a 6 digit hex value.")
    private String colour;

    private Boolean closed;

    @Pattern(regexp="^[0-9A-Z]{4}$",message="Transfer account id must be uppercase and/or numbers of length 4.")
    private String transferAccountId;

    private Integer transferDay;

    private AdjustmentType weekendAdj;

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
