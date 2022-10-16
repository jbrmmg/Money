package com.jbr.middletier.money.schedule;

public enum AdjustmentType {
    AT_FORWARD("FW"),
    AT_BACKWARD("BW"),
    AT_NONE("NO");

    private final String type;

    AdjustmentType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static AdjustmentType getAdjustmentType(String name) {
        for(AdjustmentType type : AdjustmentType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Adjustment type");
    }
}
