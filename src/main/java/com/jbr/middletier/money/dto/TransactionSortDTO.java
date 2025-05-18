package com.jbr.middletier.money.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TransactionSortDTO {
    private TransactionSortField field;
    private TransactionSortType type;

    public TransactionSortDTO() {
        this.field = TransactionSortField.DATE;
        this.type = TransactionSortType.ASCENDING;
    }

    public TransactionSortDTO(TransactionSortField field, TransactionSortType type) {
        this.field = field;
        this.type = type;
    }

}
