package com.jbr.middletier.money.dto;

import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public class ReconcileFileDataUpdateDTO {
    private LocalDateTime updateTime;
    @Pattern(regexp="^[0-9a-zA-Z_./\\\\]{1,40}",message="Path can only contain letters or digits up to 45 characters.")
    private String path;

    public ReconcileFileDataUpdateDTO(LocalDateTime updateTime,
                                      String path) {
        this.updateTime = updateTime;
        this.path = path;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
