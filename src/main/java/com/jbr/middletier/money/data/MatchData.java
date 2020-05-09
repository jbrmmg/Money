package com.jbr.middletier.money.data;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jason on 11/04/17.
 */
@SuppressWarnings({"unused", "NullableProblems"})
public class MatchData implements Comparable {
    @Override
    public int compareTo(@NotNull Object object) {
        if (!(object instanceof MatchData))
            throw new ClassCastException("A MatchData object expected.");

        MatchData anotherMatch = (MatchData)object;

        if((this.reconciliationId == anotherMatch.reconciliationId) && (this.transaction.getId() == anotherMatch.transaction.getId())) {
            return 0;
        }

        // Check action.
        if(this.forwardActionType.ordinal() > anotherMatch.forwardActionType.ordinal()) {
            return 1;
        } else if(this.forwardActionType.ordinal() < anotherMatch.forwardActionType.ordinal()) {
            return -1;
        }

        // If the action is set category, the use the description to sort.
        if(this.forwardActionType == ForwardActionType.SETCATEGORY) {
            int descriptionCompare = this.description.compareTo(anotherMatch.description);
            if (descriptionCompare != 0) {
                return descriptionCompare;
            }
        }

        // Check the date.
        int dateCompare = this.reconcilationDate.compareTo(anotherMatch.reconcilationDate);
        if (dateCompare != 0) {
            return dateCompare;
        }

        // Check the amount.
        return Double.compare(this.reconciliationAmount, anotherMatch.reconciliationAmount);

    }

    public enum ForwardActionType { SETCATEGORY, CREATE, RECONCILE, UNRECONCILE, NONE }

    private enum BackwordActionType { UNRECONCILE, DELETE, NONE }

    private final int reconciliationId;
    private final Date reconcilationDate;
    private final double reconciliationAmount;
    private Transaction transaction;
    private double beforeAmount;
    private double afterAmount;
    private Category category;
    private String description;
    private final Account account;
    private ForwardActionType forwardActionType;
    private BackwordActionType backwordActionType;

    public MatchData(ReconciliationData source, Account account)  {
        this.reconciliationId = source.getId();
        this.reconcilationDate = source.getDate();
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
        this.backwordActionType = BackwordActionType.NONE;
    }

    public MatchData(Transaction transaction) {
        this.transaction = transaction;
        this.reconciliationId = -1;
        this.reconcilationDate = transaction.getDate();
        this.reconciliationAmount = transaction.getAmount();
        this.beforeAmount = 0.0;
        this.afterAmount = 0.0;
        this.category = transaction.getCategory();
        this.account = transaction.getAccount();

        this.forwardActionType = ForwardActionType.UNRECONCILE;
        this.backwordActionType = BackwordActionType.NONE;
    }

    public void matchTransaction(Transaction transaction) {
        this.transaction = transaction;
        this.category = transaction.getCategory();

        if(transaction.getStatement() != null) {
            this.forwardActionType = ForwardActionType.NONE;
            this.backwordActionType = BackwordActionType.UNRECONCILE;
        } else {
            this.forwardActionType = ForwardActionType.RECONCILE;
            this.backwordActionType = BackwordActionType.DELETE;
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(this.reconcilationDate);
    }

    public String getForwardAction() {
        return forwardActionType.toString();
    }

    public String getBackwardAction() {
        return backwordActionType.toString();
    }
}
