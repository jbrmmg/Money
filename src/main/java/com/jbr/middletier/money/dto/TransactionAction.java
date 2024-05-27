package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionAction {
    UPDATE_CATEGORY("category", "fa-align-justify", "FFFFFF"),
    UPDATE_DETAILS("update", "fa-pencil", "FFFFFF"),
    RECONCILE("reconcile", "fa-check", "FFFFFF"),
    UNRECONCILE("un-reconcile", "fa-times", "FFFFFF"),
    DELETE("delete", "fa-trash", "FF0000");

    private final String action;
    private final String icon;
    private final String colour;

    TransactionAction(String action, String icon, String colour) {
        this.action = action;
        this.icon = icon;
        this.colour = colour;
    }

    public String getActionName() {
        return this.action;
    }

    public String getIcon() {
        return this.icon;
    }

    public String getColour() {
        return this.colour;
    }

    public static TransactionAction getTransactionAction(String name) {
        for(TransactionAction action : TransactionAction.values()) {
            if(action.getActionName().equalsIgnoreCase(name)) {
                return action;
            }
        }

        throw new IllegalStateException(name + " is not a valid transaction action type");
    }
}
