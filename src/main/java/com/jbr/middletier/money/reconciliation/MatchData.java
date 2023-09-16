package com.jbr.middletier.money.reconciliation;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.ReconciliationData;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.mapper.UtilityMapper;

import javax.validation.constraints.NotNull;
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

        // Check action.
        if(this.forwardActionType.ordinal() > object.forwardActionType.ordinal()) {
            return 1;
        } else if(this.forwardActionType.ordinal() < object.forwardActionType.ordinal()) {
            return -1;
        }

        // If the action is set category, the use the description to sort.
        if(this.forwardActionType == ForwardActionType.SETCATEGORY) {
            int descriptionCompare = this.description.compareTo(object.description);
            if (descriptionCompare != 0) {
                return descriptionCompare;
            }
        }

        // Check the date.
        int dateCompare = this.reconciliationDate.compareTo(object.reconciliationDate);
        if (dateCompare != 0) {
            return dateCompare;
        }

        // Check the amount.
        return Double.compare(this.reconciliationAmount, object.reconciliationAmount);
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
        return getId() + "-" + getDate() + "-" + getAmount() + "-" + getForwardAction();
    }

    public enum ForwardActionType { SETCATEGORY, CREATE, RECONCILE, UNRECONCILE, NONE }

    private enum BackwardActionType { UNRECONCILE, DELETE, NONE }

    private final UtilityMapper utilityMapper;
    private final int reconciliationId;
    private final LocalDate reconciliationDate;
    private final double reconciliationAmount;
    private Transaction transaction;
    private double beforeAmount;
    private double afterAmount;
    private Category category;
    private String description;
    private final Account account;
    private ForwardActionType forwardActionType;
    private BackwardActionType backwordActionType;

    public MatchData(UtilityMapper utilityMapper, ReconciliationData source, Account account)  {
        this.utilityMapper = utilityMapper;
        this.reconciliationId = source.getId();
        this.reconciliationDate = source.getDate();
        this.reconciliationAmount = source.getAmount();
        this.transaction = null;
        this.category = source.getCategory();
        this.description = source.getDescription();
        this.account = account;

        // Set the forward action.
        if(this.category != null) {
            this.forwardActionType = ForwardActionType.CREATE;
        } else {
            this.forwardActionType = ForwardActionType.SETCATEGORY;
        }
        this.backwordActionType = BackwardActionType.NONE;
    }

    public MatchData(UtilityMapper utilityMapper, Transaction transaction) {
        this.utilityMapper = utilityMapper;
        this.transaction = transaction;
        this.reconciliationId = -1;
        this.reconciliationDate = transaction.getDate();
        this.reconciliationAmount = transaction.getAmount().getValue();
        this.beforeAmount = 0.0;
        this.afterAmount = 0.0;
        this.category = transaction.getCategory();
        this.account = transaction.getAccount();

        this.forwardActionType = ForwardActionType.UNRECONCILE;
        this.backwordActionType = BackwardActionType.NONE;
    }

    public void matchTransaction(Transaction transaction) {
        this.transaction = transaction;
        this.category = transaction.getCategory();

        if(transaction.getStatement() != null) {
            this.forwardActionType = ForwardActionType.NONE;
            this.backwordActionType = BackwardActionType.UNRECONCILE;
        } else {
            this.forwardActionType = ForwardActionType.RECONCILE;
            this.backwordActionType = BackwardActionType.DELETE;
        }
    }

    public int getId() {
        return this.reconciliationId;
    }

    public double getAmount() {
        return this.reconciliationAmount;
    }

    public Transaction getTransaction() {
        return this.transaction;
    }

    public double getBeforeAmount() {
        return this.beforeAmount;
    }

    public void setBeforeAmount(double beforeTransactionAmount) {
        this.beforeAmount = beforeTransactionAmount;
    }

    public double getAfterAmount() {
        return this.afterAmount;
    }

    public void setAfterAmount(double afterTransactionAmount) {
        this.afterAmount = afterTransactionAmount;
    }

    public Category getCategory() { return this.category; }

    public String getDescription() { return this.description; }

    public Account getAccount() { return this.account; }

    public String getDate() {
        return utilityMapper.map(this.reconciliationDate,String.class);
    }

    public boolean transactionMatch(Transaction transaction, int withinDays) {
        // If the amount does not match then there is no match.
        double epsilon = 0.001d;
        if(Math.abs(this.reconciliationAmount - transaction.getAmount().getValue()) > epsilon) {
            return false;
        }

        if(withinDays == 0) {
            return transaction.getDate().equals(this.reconciliationDate);
        }

        // Transaction Date must be within the number of days of the reconciliation date.
        LocalDate startDate = this.reconciliationDate.minusDays(withinDays);
        LocalDate endDate = this.reconciliationDate.plusDays(withinDays);
        return transaction.getDate().isAfter(startDate) && transaction.getDate().isBefore(endDate);
    }

    public String getForwardAction() {
        return forwardActionType.toString();
    }

    public String getBackwardAction() {
        return backwordActionType.toString();
    }
}
