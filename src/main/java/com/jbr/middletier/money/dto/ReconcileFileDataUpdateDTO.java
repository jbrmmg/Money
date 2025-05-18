package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ReconcileFileDataUpdateDTO {
    private LocalDateTime updateTime;
    @Pattern(regexp="^[\\da-zA-Z_./\\\\]{1,40}",message="Path can only contain letters or digits up to 45 characters.")
    private String path;

    public ReconcileFileDataUpdateDTO(LocalDateTime updateTime,
                                      String path) {
        this.updateTime = updateTime;
        this.path = path;
    }
}
