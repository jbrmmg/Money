package com.jbr.middletier.money.data;

public enum TransactionRequestType {
    TRT_UNRECONCILED("UN"),
    TRT_RECONCILED("RC"),
    TRT_ALL("AL"),
    TRT_UNLOCKED("UL"),
    TRT_UNKNOWN("XX");

    private final String type;

    TransactionRequestType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static TransactionRequestType getTransactionType(String name) {
        for(TransactionRequestType type : TransactionRequestType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Transaction Request type");
    }
}
