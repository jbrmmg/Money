package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TransactionReportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportManager.class);

    private final AccountTransactionManager transactionManager;
    private final RegularPaymentManager regularPaymentManager;
    private final TransactionFilter filter;

    @Autowired
    public TransactionReportManager(AccountTransactionManager transactionManager,
                                    RegularPaymentManager regularPaymentManager,
                                    TransactionFilter filter) {
        this.transactionManager = transactionManager;
        this.regularPaymentManager = regularPaymentManager;
        this.filter = filter;
    }

    private String getDateString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private List<TransactionReportDTO> getPredicted(TransactionFilterDTO filter) {
        // If predicted are excluded then return empty list.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.FALSE)) {
            return new ArrayList<>();
        }

        ArrayList<TransactionReportDTO> result = new ArrayList<>();

        for(Regular next : regularPaymentManager.getAllRegularPayments()) {
            this.filter.passTransaction(next, filter).ifPresent(result::add);
        }

        return result;
    }

    private List<TransactionReportDTO> getFromReconciled(TransactionFilterDTO filter) {
        // If predicted are excluded then return empty list.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.FALSE)) {
            return new ArrayList<>();
        }

        return new ArrayList<>();
    }

    private List<TransactionReportDTO> getStandardTransactions(TransactionFilterDTO filter) {
        // If predicated or from reconciled then we don't need standard tranactions.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.TRUE)) {
            return new ArrayList<>();
        }

        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.TRUE)) {
            return new ArrayList<>();
        }

        ArrayList<TransactionReportDTO> result = new ArrayList<>();

        for(Transaction next : transactionManager.getAllTransactions()) {
            this.filter.passTransaction(next, filter).ifPresent(result::add);
        }

        return result;
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
        LOG.info("Get Transactions based on {}",filter);

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
