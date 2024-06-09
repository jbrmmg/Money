package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TransactionSortField {
    @JsonEnumDefaultValue DATE,
    AMOUNT,
    ACCOUNT,
    CATEGORY,
    DESCRIPTION
}
