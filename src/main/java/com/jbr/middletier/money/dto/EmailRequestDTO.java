package com.jbr.middletier.money.dto;

import javax.validation.constraints.*;

public class EmailRequestDTO {
    @Email(message="To must be a valid email address")
    @NotNull(message="To cannot be null")
    private String to;

    @NotNull(message="From cannot be null")
    @Email(message="From must be a valid email address")
    private String from;

    @NotNull(message="Username cannot be null")
    @Email(message="Username must be a valid email address")
    private String username;

    @Pattern(regexp="^[0-9a-zA-Z._]{1,200}$",message="Hostname must be alphanumeric (plus . or _) upto 200 characters")
    private String host;

    @Pattern(regexp="^[0-9a-zA-Z._]{1,200}$",message="Hostname must be alphanumeric (plus . or _) upto 200 characters")
    private String password;

    @Max(value = 52,message="Weeks cannot be greater than 52")
    @Min(value = 2,message="Weeks cannot be less than 2")
    private Integer weeks;

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getWeeks() {
        // If non specified then the default is 7.
        if(this.weeks == null) {
            return 7;
        }
        return weeks;
    }

    public void setWeeks(Integer weeks) {
        this.weeks = weeks;
    }

    @Override
    public String toString() {
        return "Email - To: " + this.getTo() + " From: " + this.getFrom() + " for " + this.getWeeks();
    }
}
