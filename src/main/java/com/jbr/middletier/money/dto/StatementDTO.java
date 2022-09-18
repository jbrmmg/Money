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
        // First compare the account.
        if(!this.id.getAccount().getId().equalsIgnoreCase(o.id.getAccount().getId())) {
            return this.id.getAccount().getId().compareTo(o.id.getAccount().getId());
        }

        // Then compare the year
        if(!this.id.getYear().equals(o.id.getYear())) {
            return this.id.getYear().compareTo(o.id.getYear());
        }

        // Finally the month
        if(!this.id.getMonth().equals(o.id.getMonth())) {
            return this.id.getMonth().compareTo(o.id.getMonth());
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof StatementDTO)) {
            return false;
        }

        return compareTo((StatementDTO) obj) == 0;
    }
}
