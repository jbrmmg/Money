package com.jbr.middletier.money.data;

public class UpdateTransaction {
    private int id;
    private double amount;
    private String category;
    private String description;

    public UpdateTransaction() {
        this.id = -1;
        this.amount = 0.0;
        this.category = "";
        this.description = "";
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() { return this.category; }

    public String getDescription() { return this.description; }

    public void setId(int id) {
        this.id = id;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setCategory(String category) { this.category = category; }

    public void setDescription(String description) { this.description = description; }
}
