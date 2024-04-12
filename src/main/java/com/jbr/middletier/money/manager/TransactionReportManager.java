package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TransactionReportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportManager.class);

    private String getDateString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private boolean transctionPassFilterValue(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        return true;
    }

    private boolean transctionPassFilterDate(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        return true;
    }

    private boolean transctionPassFilterCategory(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        return true;
    }

    private boolean transctionPassFilterAccount(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        return true;
    }

    private boolean transctionPassFilterStatement(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        return true;
    }

    private TransactionReportDTO transactionPassesFilter(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // Make sure the transaction passes the filters.
        if(!transctionPassFilterValue(transaction, filter)) {
            return null;
        }

        if(!transctionPassFilterDate(transaction, filter)) {
            return null;
        }

        if(!transctionPassFilterAccount(transaction, filter)) {
            return null;
        }

        if(!transctionPassFilterCategory(transaction, filter)) {
            return null;
        }

        if(!transctionPassFilterStatement(transaction, filter)) {
            return null;
        }

        return transaction;
    }

    private List<TransactionReportDTO> getPredicted(TransactionFilterDTO filter) {
        // If predicted are excluded then return empty list.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.FALSE)) {
            return new ArrayList<TransactionReportDTO>();
        }

        return new ArrayList<TransactionReportDTO>();
    }

    private List<TransactionReportDTO> getFromReconciled(TransactionFilterDTO filter) {
        // If predicted are excluded then return empty list.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.FALSE)) {
            return new ArrayList<TransactionReportDTO>();
        }

        return new ArrayList<TransactionReportDTO>();
    }

    private List<TransactionReportDTO> getStandardTransactions(TransactionFilterDTO filter) {
        // If predicated or from reconciled then we don't need standard tranactions.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.TRUE)) {
            return new ArrayList<TransactionReportDTO>();
        }

        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.TRUE)) {
            return new ArrayList<TransactionReportDTO>();
        }

        return new ArrayList<TransactionReportDTO>();
    }

    private FinancialAmount calculateOpeningBalance() {
        return new FinancialAmount();
    }

    private String calculateOpenDate(List<TransactionReportDTO> transactions) {
        return "";
    }

    private FinancialAmount calculateTodayBalance(FinancialAmount openBalance, List<TransactionReportDTO> transactions) {
        return new FinancialAmount();
    }

    private String calculateFutureDate() {
        return "";
    }

    private FinancialAmount calculateFutureBalance(FinancialAmount openBalance, List<TransactionReportDTO> transactions) {
        return new FinancialAmount();
    }

    public TransactionDataDTO getTransactions(TransactionFilterDTO filter) {
        TransactionDataDTO result = new TransactionDataDTO();

        // Add the transactions.
        result.getTransactions().addAll(getPredicted(filter));
        result.getTransactions().addAll(getFromReconciled(filter));
        result.getTransactions().addAll(getStandardTransactions(filter));

        // Merge any transactions that actually represent the same thing - i.e. where reconciled and created.

        // Sort the transactions.

        // Calculate the opening details
        result.setOpenBalance(calculateOpeningBalance());
        result.setOpenDate(calculateOpenDate(result.getTransactions()));

        // Calculate the balances on the transactions.

        // Calculate today's date.
        result.setToday(getDateString(LocalDate.now()));

        // Calculate today's balance.
        result.setTodayBalance(calculateTodayBalance(result.getOpenBalance(), result.getTransactions()));

        // Calculate future date.
        result.setForwardDate(calculateFutureDate());

        // Calculate future balance.
        result.setForwardBalance(calculateFutureBalance(result.getOpenBalance(), result.getTransactions()));

        // Return the data.
        return result;
    }
}
