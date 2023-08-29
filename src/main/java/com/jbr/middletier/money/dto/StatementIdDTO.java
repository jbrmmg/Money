package com.jbr.middletier.money.dto;

import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class StatementIdDTO implements Comparable<StatementIdDTO> {
    String accountId;
    @Min(1)
    @Max(12)
    Integer month;
    @Min(1)
    @Max(9999)
    Integer year;

    public StatementIdDTO(String accountId,
                          Integer month,
                          Integer year) {
        this.accountId = accountId;
        this.month = month;
        this.year = year;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    @Override
    public int compareTo(@NotNull StatementIdDTO o) {
        // First compare the account.
        if(!this.accountId.equalsIgnoreCase(o.accountId)) {
            return this.accountId.compareTo(o.accountId);
        }

        // Then compare the year
        if(!this.year.equals(o.year)) {
            return this.year.compareTo(o.year);
        }

        // Finally the month
        if(!this.month.equals(o.month)) {
            return this.month.compareTo(o.month);
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof StatementIdDTO)) {
            return false;
        }

        return compareTo((StatementIdDTO) obj) == 0;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.accountId + "." + String.format("%04d", this.year) + String.format("%02d", this.month);
    }
}
