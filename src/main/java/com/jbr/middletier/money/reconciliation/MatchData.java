package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.ReconciliationData;
import com.jbr.middletier.money.data.primary.Transaction;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Created by jason on 11/04/17.
 */
public class MatchData implements Comparable<MatchData> {
    @Override
    public int compareTo(@NotNull MatchData object) {
        if((this.reconciliationId == object.reconciliationId) && (this.transaction.getId() == object.transaction.getId())) {
            return 0;
        }

        // Check the date.
        int dateCompare = this.reconciliationDate.compareTo(object.reconciliationDate);
        if (dateCompare != 0) {
            return dateCompare;
        }

        // Check the amount.
        return this.reconciliationAmount.compareTo(object.reconciliationAmount);
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) return true;

        if (!(object instanceof MatchData other)) {
            return false;
        }

        return this.compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        return getId() + "-" + getDate() + "-" + getAmount();
    }

    private final int reconciliationId;
    private final LocalDate reconciliationDate;
    private final BigDecimal reconciliationAmount;
    private final String description;
    private Transaction transaction;
    private Category category;
    private final Account account;

    public MatchData(ReconciliationData source, Account account)  {
        this.reconciliationId = source.getId();
        this.reconciliationDate = source.getDate();
        this.reconciliationAmount = source.getAmount();
        this.transaction = null;
        this.category = source.getCategory();
        this.description = source.getDescription();
        this.account = account;
    }

    public MatchData(Transaction transaction) {
        this.transaction = transaction;
        this.reconciliationId = -1;
        this.reconciliationDate = transaction.getDate();
        this.reconciliationAmount = transaction.getAmount().getValue();
        this.category = transaction.getCategory();
        this.account = transaction.getAccount();
        this.description = transaction.getDescription();
    }

    public void matchTransaction(Transaction transaction) {
        this.transaction = transaction;
        this.category = transaction.getCategory();
    }

    public int getId() {
        return this.reconciliationId;
    }

    public BigDecimal getAmount() {
        return this.reconciliationAmount;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public Category getCategory() { return this.category; }

    public String getDescription() { return this.description; }

    public Account getAccount() { return this.account; }

    public LocalDate getDate() {
        return this.reconciliationDate;
    }

    public boolean transactionMatch(Transaction transaction, int withinDays) {
        // If the amount does not match then there is no match.
        double epsilon = 0.001d;
        if(this.reconciliationAmount.subtract(transaction.getAmount().getValue()).abs().compareTo(BigDecimal.valueOf(epsilon)) > 0) {
            return false;
        }

        if(withinDays == 0) {
            return transaction.getDate().equals(this.reconciliationDate);
        }

        // Transaction Date must be within the number of days of the reconciliation date.
        LocalDate startDate = this.reconciliationDate.minusDays(withinDays);
        LocalDate endDate = this.reconciliationDate.plusDays(withinDays);

        return startDate.equals(transaction.getDate()) || endDate.equals(transaction.getDate());
    }
}
