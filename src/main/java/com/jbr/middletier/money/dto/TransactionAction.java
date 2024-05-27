package com.jbr.middletier.money.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jbr.middletier.money.config.Constants;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TransactionAction {
    UPDATE_CATEGORY("category", "fa-align-justify", Constants.COLOUR_WHITE),
    UPDATE_DETAILS("update", "fa-pencil", Constants.COLOUR_WHITE),
    RECONCILE("reconcile", "fa-check", Constants.COLOUR_WHITE),
    UNRECONCILE("un-reconcile", "fa-times", Constants.COLOUR_WHITE),
    DELETE("delete", "fa-trash", Constants.COLOUR_RED);

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
