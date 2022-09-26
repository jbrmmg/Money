package com.jbr.middletier.money.dto;

import com.jbr.middletier.money.util.FinancialAmount;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TransactionWindowDTO {
    private LocalDate start;
    private FinancialAmount startBalance;
    private List<TransactionDTO> transactions;

    public TransactionWindowDTO() {
        this.start = LocalDate.now();
        this.startBalance = new FinancialAmount(0);
        this.transactions = new ArrayList<>();
    }

    public LocalDate getStart() {
        return start;
    }

    public void setStart(LocalDate start) {
        this.start = start;
    }

    public FinancialAmount getStartBalance() {
        return startBalance;
    }

    public void setStartBalance(double startBalance) {
        this.startBalance = new FinancialAmount(startBalance);
    }

    public List<TransactionDTO> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionDTO> transactions) {
        this.transactions = transactions;
    }
}
