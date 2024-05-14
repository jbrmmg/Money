package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

public class TransactionDataDTO {
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Open date must be a date in format yyyy-dd-mm")
    private String openDate;
    private FinancialAmount openBalance;

    private List<TransactionReportDTO> transactions;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Today must be a date in format yyyy-dd-mm")
    private String today;
    private FinancialAmount todayBalance;

    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$",message = "Forward date must be a date in format yyyy-dd-mm")
    private String forwardDate;
    private FinancialAmount forwardBalance;

    public TransactionDataDTO() {
        this.transactions = new ArrayList<>();
    }

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public FinancialAmount getOpenBalance() {
        return openBalance;
    }

    public void setOpenBalance(FinancialAmount openBalance) {
        this.openBalance = openBalance;
    }

    public List<TransactionReportDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionReportDTO> transactions) {
        this.transactions = transactions;
    }

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public FinancialAmount getTodayBalance() {
        return todayBalance;
    }

    public void setTodayBalance(FinancialAmount todayBalance) {
        this.todayBalance = todayBalance;
    }

    public String getForwardDate() {
        return forwardDate;
    }

    public void setForwardDate(String forwardDate) {
        this.forwardDate = forwardDate;
    }

    public FinancialAmount getForwardBalance() {
        return forwardBalance;
    }

    public void setForwardBalance(FinancialAmount forwardBalance) {
        this.forwardBalance = forwardBalance;
    }

    public void removeDuplicatesAndSort() {
        List<TransactionReportDTO> duplicates = new ArrayList<>();

        // Look for duplicates
        for(TransactionReportDTO transaction : this.transactions) {
            if(transaction.getFromReconciliation().equals(Boolean.TRUE) && transaction.getId() != null) {
                // Find the transaction.
                for(TransactionReportDTO innerTransaction : this.transactions) {
                    if(!innerTransaction.getFromReconciliation().equals(Boolean.TRUE) && innerTransaction.getPredicted().equals(Boolean.FALSE) && innerTransaction.getId().equals(transaction.getId())) {
                        duplicates.add(transaction);
                    }
                }
            }
        }

        // Remove duplicates
        this.transactions.removeAll(duplicates);

        // Sort the transactions.
        this.transactions.sort((t1,t2) -> {
            // Sort first by date.
            if(!t1.getDate().equals(t2.getDate())) {
                return t1.getDate().compareTo(t2.getDate());
            }

            // Sort by the amounts.
            return Double.compare(t1.getAmount().getValue(),t2.getAmount().getValue());
        });
    }
}
