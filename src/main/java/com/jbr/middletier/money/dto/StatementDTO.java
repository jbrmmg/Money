package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.Serial;
import java.io.Serializable;

@Setter
public class StatementDTO implements Comparable<StatementDTO>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    @Pattern(regexp="^[\\da-zA-Z]{1,4}$",message="Account ID contain letters or digits up to 4 characters.")
    private String accountId;

    @Getter
    @Min(1)
    @Max(12)
    private Integer month;

    @Getter
    @Min(1900)
    @Max(2399)
    private Integer year;

    private FinancialAmount openBalance;

    @Getter
    private Integer age;

    private boolean locked;

    public FinancialAmount getOpenBalance() {
        if(this.openBalance == null) {
            return new FinancialAmount();
        }

        return openBalance;
    }

    public boolean getLocked() {
        return locked;
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

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if(this.accountId != null) {
            result.append(this.accountId);
        }

        if(this.year != null) {
            result.append(" ");
            result.append(this.year);
        }

        if(this.month != null) {
            result.append(" ");
            result.append(this.month);
        }

        return result.toString();
    }
}
