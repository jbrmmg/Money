package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Controller
public class TransactionFilter {
    private final TransactionMapper transactionMapper;

    @Autowired
    public TransactionFilter(TransactionMapper transactionMapper) {
        this.transactionMapper = transactionMapper;
    }

    private boolean transactionPassFilterLocked(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getLocked() == null) {
            return true;
        }

        // Get the locked status.
        boolean transactionLocked = false;

        if(transaction.getStatement() != null) {
            if(transaction.getStatement().getLocked()) {
                transactionLocked = true;
            }
        }

        return transactionLocked == filter.getLocked();
    }

    private boolean transactionPassFilterValue(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getValueRange() == null) {
            return true;
        }

        return (!(transaction.getAmount().getValue() > filter.getValueRange().getMaximum())) &&
                (!(transaction.getAmount().getValue() < filter.getValueRange().getMinimum()));
    }

    private LocalDate dateStringToDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private boolean transctionPassFilterDate(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // If there is no date filter then pass the filter.
        if(filter.getDateRange() == null) {
            return true;
        }

        LocalDate transactionDate = dateStringToDate(transaction.getDate());

        // Check the minimum date if available.
        if(filter.getDateRange().getFrom() != null) {
            LocalDate from = dateStringToDate(filter.getDateRange().getFrom());

            if(from.isAfter(transactionDate)) {
                return false;
            }
        }

        if(filter.getDateRange().getTo() != null) {
            LocalDate to = dateStringToDate(filter.getDateRange().getTo());

            return !to.isBefore(transactionDate);
        }

        return true;
    }

    private boolean transactionPassFilterCategory(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // If there is no filter on category then pass.
        if(filter.getCategories().isEmpty()) {
            return true;
        }

        for(CategoryDTO category : filter.getCategories()) {
            if(transaction.getCategory().getId().equals(category.getId())) {
                return true;
            }
        }

        return false;
    }

    private boolean transactionPassFilterAccount(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // If account list is empty then any account is allowed.
        if(filter.getAccounts().isEmpty()) {
            return true;
        }

        for(AccountDTO next : filter.getAccounts()) {
            if(transaction.getAccount().getId().equalsIgnoreCase(next.getId())) {
                return true;
            }
        }

        return false;
    }

    private boolean transactionPassFilterStatement(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getStatementDate() == null) {
            return true;
        }

        if(filter.getStatementDate().getMonth() != null) {
            if(!filter.getStatementDate().getMonth().equals(transaction.getStatement().getMonth())) {
                return false;
            }
        }

        if(filter.getStatementDate().getYear() != null) {
            return filter.getStatementDate().getYear().equals(transaction.getStatement().getYear());
        }

        return true;
    }

    private Optional<TransactionReportDTO> internalPassTransaction(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // Make sure the transaction passes the filters.
        if(!transactionPassFilterLocked(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterValue(transaction, filter)) {
            return Optional.empty();
        }

        if(!transctionPassFilterDate(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterAccount(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterCategory(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterStatement(transaction, filter)) {
            return Optional.empty();
        }

        return Optional.of(transaction);
    }

    public Optional<TransactionReportDTO> passTransaction(Regular transaction, TransactionFilterDTO filter) {
        TransactionReportDTO result = transactionMapper.map(transaction,TransactionReportDTO.class);

        // Only allowed if predicted are allowed.
        if(filter.getPredicted() != null && filter.getPredicted() == Boolean.FALSE) {
            return Optional.empty();
        }

        // If the filter only wants data from reconciliation then result is empty.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.TRUE)) {
            return Optional.empty();
        }

        return internalPassTransaction(result,filter);
    }

    public Optional<TransactionReportDTO> passTransaction(Transaction transaction, TransactionFilterDTO filter) {
        TransactionReportDTO result = transactionMapper.map(transaction,TransactionReportDTO.class);

        // If the filter only wants predicted then result empty.
        if(filter.getPredicted() != null && filter.getPredicted() == Boolean.TRUE) {
            return Optional.empty();
        }

        // If the filter only wants data from reconciliation then result is empty.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.TRUE)) {
            return Optional.empty();
        }

        return internalPassTransaction(result,filter);
    }

    public Optional<TransactionReportDTO> passTransaction(MatchData transaction, TransactionFilterDTO filter) {
        TransactionReportDTO result = transactionMapper.map(transaction,TransactionReportDTO.class);

        // If the filter only wants predicted then result empty.
        if(filter.getPredicted() != null && filter.getPredicted() == Boolean.TRUE) {
            return Optional.empty();
        }

        // If the filter only wants data from reconciliation then result is empty.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.FALSE)) {
            return Optional.empty();
        }

        return internalPassTransaction(result,filter);
    }
}
