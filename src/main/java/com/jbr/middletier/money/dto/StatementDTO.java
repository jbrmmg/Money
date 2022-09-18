package com.jbr.middletier.money.dto;

public class StatementDTO implements Comparable<StatementDTO> {
    private StatementIdDTO id;
    private double openBalance;
    private boolean locked;

    public StatementIdDTO getId() {
        return id;
    }

    public void setId(StatementIdDTO id) {
        this.id = id;
    }

    public double getOpenBalance() {
        return openBalance;
    }

    public void setOpenBalance(double openBalance) {
        this.openBalance = openBalance;
    }

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public int compareTo(final StatementDTO o) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof StatementDTO)) {
            return false;
        }

        return compareTo((StatementDTO) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
