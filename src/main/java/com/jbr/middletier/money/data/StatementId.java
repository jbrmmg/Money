package com.jbr.middletier.money.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by jason on 07/03/17.
 */
public class StatementId implements Serializable {
    private String account;
    private Integer month;
    private Integer year;

    public StatementId(String account, int year, int month) {
        this.account = account;
        this.month = month;
        this.year = year;
    }

    public StatementId(){}

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (!(o instanceof StatementId)) {
            return false;
        }

        StatementId statementId = (StatementId)o;

        return statementId.account.equalsIgnoreCase(this.account) && Objects.equals(statementId.month, this.month) && Objects.equals(statementId.year, this.year);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + month;
        result = 31 * result + year;
        result = 31 * result + account.hashCode();
        return result;
    }
}
