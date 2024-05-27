package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.reconciliation.MatchData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.time.LocalDate;
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
        boolean transactionLocked = transaction.getStatement() != null && transaction.getStatement().getLocked();

        return transactionLocked == filter.getLocked();
    }

    private boolean transactionPassFilterValue(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getValueRange() == null) {
            return true;
        }

        return ((transaction.getAmount().getValue() <= filter.getValueRange().getMaximum()) &&
                (transaction.getAmount().getValue() >= filter.getValueRange().getMinimum()));
    }

    private LocalDate dateStringToDate(String date) {
        return LocalDate.parse(date, Constants.MONEY_DATE_FORMATTER);
    }

    private boolean transactionPassFilterDate(TransactionReportDTO transaction, TransactionFilterDTO filter) {
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

        if(filter.getStatementDate().getMonth() != null && !filter.getStatementDate().getMonth().equals(transaction.getStatement().getMonth())) {
            return false;
        }

        if(filter.getStatementDate().getYear() != null) {
            return filter.getStatementDate().getYear().equals(transaction.getStatement().getYear());
        }

        return true;
    }

    private boolean transactionPassDescription(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getDescription() == null) {
            return true;
        }

        // Check if the description contains the filter.
        String description = transaction.getDescription().replaceAll("\\s+", " ");
        description = description.replaceAll("[^\\da-zA-Z ]", "").toLowerCase();

        return description.contains(filter.getDescription().toLowerCase());
    }

    private Optional<TransactionReportDTO> internalPassTransaction(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        // Make sure the transaction passes the filters.
        if(!transactionPassFilterLocked(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterValue(transaction, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterDate(transaction, filter)) {
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

        if(!transactionPassDescription(transaction, filter)) {
            return Optional.empty();
        }

        // Set the actions that are allowed.
        if(Boolean.TRUE.equals(transaction.getFromReconciliation()) && transaction.getId() == null) {
            transaction.addAction(TransactionAction.UPDATE_CATEGORY);
        } else if (Boolean.TRUE.equals(transaction.getPredicted())) {
            transaction.addAction(TransactionAction.UPDATE_CATEGORY);
        } else if(transaction.getStatement() != null) {
            transaction.addAction(TransactionAction.UPDATE_CATEGORY);
            if(!transaction.getStatement().getLocked()) {
                transaction.addAction(TransactionAction.UNRECONCILE);
            }
        } else {
            transaction.addAction(TransactionAction.DELETE);
            transaction.addAction(TransactionAction.UPDATE_DETAILS);
            transaction.addAction(TransactionAction.RECONCILE);
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
