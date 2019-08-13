package com.jbr.middletier.money.data;

public class UpdateTransaction {
    private int id;
    private double amount;

    public UpdateTransaction() {
        this.id = -1;
        this.amount = 0.0;
    }

    public int getId() {
        return id;
    }

    public double getAmount() {
        return amount;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
