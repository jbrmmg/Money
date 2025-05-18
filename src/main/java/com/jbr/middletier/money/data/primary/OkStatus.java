package com.jbr.middletier.money.data.primary;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OkStatus {
    private String status;

    private OkStatus() {
        status = "OK";
    }

    public static OkStatus getOkStatus() {
        return new OkStatus();
    }
}
