package com.jbr.middletier.money.dto;

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

    public TransactionSortField getField() {
        return field;
    }

    public void setField(TransactionSortField field) {
        this.field = field;
    }

    public TransactionSortType getType() {
        return type;
    }

    public void setType(TransactionSortType type) {
        this.type = type;
    }
}
