package com.jbr.middletier.money.dto;

@SuppressWarnings("unused")
public class UpdateTransactionDTO {
    private int id;
    private double amount;
    private String categoryId;
    private String description;

    public UpdateTransactionDTO() {
        this.id = -1;
        this.amount = 0.0;
        this.categoryId = "";
        this.description = "";
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategoryId() { return this.categoryId; }

    public String getDescription() { return this.description; }

    public void setId(int id) {
        this.id = id;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategoryId(String category) { this.categoryId = category; }

    public void setDescription(String description) { this.description = description; }
}
