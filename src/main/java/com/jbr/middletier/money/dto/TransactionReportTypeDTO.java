package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TransactionReportTypeDTO {
    @JsonEnumDefaultValue TRANSACTION,
    OPEN_BALANCE,
    TODAY_BALANCE,
    FUTURE_BALANCE
}
