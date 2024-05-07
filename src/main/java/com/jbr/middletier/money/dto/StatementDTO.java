package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;

public class StatementDTO implements Comparable<StatementDTO> {
    private String accountId;
    private Integer month;
    private Integer year;
    private FinancialAmount openBalance;
    private boolean locked;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String id) {
        this.accountId = id;
    }

    public Integer getMonth() {
        return this.month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return this.year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public FinancialAmount getOpenBalance() {
        if(this.openBalance == null) {
            return new FinancialAmount();
        }

        return openBalance;
    }

    public void setOpenBalance(FinancialAmount openBalance) {
        this.openBalance = openBalance;
    }

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    private StatementIdDTO statementIdDTO() {
        return new StatementIdDTO(this.accountId,this.month,this.year);
    }

    @Override
    public int compareTo(final StatementDTO o) {
        return statementIdDTO().compareTo(o.statementIdDTO());
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
        return this.statementIdDTO().hashCode();
    }
}
