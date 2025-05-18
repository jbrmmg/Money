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

    @Getter
    @NotNull(message="From cannot be null")
    @Email(message="From must be a valid email address")
    private String from;

    @Getter
    @NotNull(message="Username cannot be null")
    @Email(message="Username must be a valid email address")
    private String username;

    @Getter
    @Pattern(regexp="^[\\da-zA-Z._]{1,200}$",message="Hostname must be alphanumeric (plus . or _) upto 200 characters")
    private String host;

    @Getter
    @Pattern(regexp="^[\\da-zA-Z._]{1,200}$",message="Hostname must be alphanumeric (plus . or _) upto 200 characters")
    private String password;

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
        return "Email - To: " + this.getTo() + " From: " + this.getFrom() + " for " + this.getWeeks();
    }
}
