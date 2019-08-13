package com.jbr.middletier.money.data;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jason on 11/04/17.
 */
public class MatchData implements Comparable {
    @Override
    public int compareTo(@SuppressWarnings("NullableProblems") final Object object) {
        if (!(object instanceof MatchData))
            throw new ClassCastException("A MatchData object expected.");

        MatchData anotherMatch = (MatchData)object;

        if((this.reconciliationId == anotherMatch.reconciliationId) && (this.transactionId == anotherMatch.transactionId)) {
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
        if(this.reconciliationAmount > anotherMatch.reconciliationAmount) {
            return 1;
        }
        if(this.reconciliationAmount < anotherMatch.reconciliationAmount) {
            return -1;
        }

        return 0;
    }

    public enum ForwardActionType { SETCATEGORY, CREATE, RECONCILE, UNRECONCILE, NONE }

    private enum BackwordActionType { UNRECONCILE, DELETE, NONE }

    private final int reconciliationId;
    private final Date reconcilationDate;
    private final double reconciliationAmount;
    private int transactionId;
    private double beforeAmount;
    private double afterAmount;
    private String category;
    private String description;
    private String colour;
    private final String account;
    private ForwardActionType forwardActionType;
    private BackwordActionType backwordActionType;

    public MatchData(ReconciliationData source, String account)  {
        this.reconciliationId = source.getId();
        this.reconcilationDate = source.getDate();
        this.reconciliationAmount = source.getAmount();
        this.transactionId = -1;
        this.category = source.getCategory();
        this.description = source.getDescription();
        this.colour = source.getColour();
        this.account = account;

        // Set the forward action.
        if(this.category != null && this.category.length() > 0) {
            this.forwardActionType = ForwardActionType.CREATE;
        } else {
            this.forwardActionType = ForwardActionType.SETCATEGORY;
        }
        this.backwordActionType = BackwordActionType.NONE;
    }

    public MatchData(AllTransaction transaction) {
        this.transactionId = transaction.getId();
        this.reconciliationId = -1;
        this.reconcilationDate = transaction.getDate();
        this.reconciliationAmount = transaction.getAmount();
        this.beforeAmount = 0.0;
        this.afterAmount = 0.0;
        this.category = transaction.getCategoryId();
        this.colour = transaction.getCatColour();
        this.account = transaction.getAccount();

        this.forwardActionType = ForwardActionType.UNRECONCILE;
        this.backwordActionType = BackwordActionType.NONE;
    }

    public void matchTransaction(AllTransaction transaction) {
        this.transactionId = transaction.getId();
        this.category = transaction.getCategoryId();
        this.colour = transaction.getCatColour();

        if(transaction.getReconciled()) {
            this.forwardActionType = ForwardActionType.NONE;
            this.backwordActionType = BackwordActionType.UNRECONCILE;
        } else {
            this.forwardActionType = ForwardActionType.RECONCILE;
            this.backwordActionType = BackwordActionType.DELETE;
        }
    }

    public void closeMatchTransaction(AllTransaction transaction) {
        this.transactionId = transaction.getId();
        this.category = transaction.getCategoryId();
        this.colour = transaction.getCatColour();

        if(transaction.getReconciled()) {
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

    public int getTransactionId() {
        return this.transactionId;
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

    public String getCategory() { return this.category; }

    public String getColour() { return this.colour; }

    public String getDescription() { return this.description; }

    public String getAccount() { return this.account; }

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
