package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TransactionSortType {
    @JsonEnumDefaultValue ASCENDING,
    DESCENDING
}
