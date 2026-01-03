package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Setter
public class EmailRequestDTO {
    @Getter
    @Email(message="To must be a valid email address")
    @NotNull(message="To cannot be null")
    private String to;

    @Max(value = 52,message="Weeks cannot be greater than 52")
    @Min(value = 2,message="Weeks cannot be less than 2")
    private Integer weeks;

    public Integer getWeeks() {
        // If non specified, then the default is 7.
        if(this.weeks == null) {
            return 7;
        }
        return weeks;
    }

    @Override
    public String toString() {
        return "Email - To: " + this.getTo() + " for " + this.getWeeks();
    }
}
