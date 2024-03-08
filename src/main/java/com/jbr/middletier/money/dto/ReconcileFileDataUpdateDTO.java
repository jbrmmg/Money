package com.jbr.middletier.money.dto;

import java.time.LocalDateTime;

public class ReconcileFileDataUpdateDTO {
    private LocalDateTime updateTime;
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
