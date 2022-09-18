package com.jbr.middletier.money.dto;

import org.jetbrains.annotations.NotNull;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class StatementIdDTO implements Comparable<StatementIdDTO> {
    AccountDTO account;
    @Min(1)
    @Max(12)
    Integer month;
    @Min(1)
    @Max(9999)
    Integer year;

    public AccountDTO getAccount() {
        return account;
    }

    public void setAccount(AccountDTO account) {
        this.account = account;
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
        if(!this.account.getId().equalsIgnoreCase(o.account.getId())) {
            return this.account.getId().compareTo(o.account.getId());
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
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.account.getId() + "." + String.format("%04d", this.year) + String.format("%02d", this.month);
    }
}
