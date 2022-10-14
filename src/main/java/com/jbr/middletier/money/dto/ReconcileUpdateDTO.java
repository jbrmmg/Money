package com.jbr.middletier.money.dto;

@SuppressWarnings("unused")
public class ReconcileUpdateDTO {
    private int id;
    private String categoryId;
    private String type;

    public ReconcileUpdateDTO() {
        this.id = -1;
        this.categoryId = "";
        this.type = "rec";
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
