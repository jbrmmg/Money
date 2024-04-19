package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.TransactionDataDTO;
import com.jbr.middletier.money.dto.TransactionFilterDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
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

    private boolean checkValueRange(double value, Double minimum, Double maximim) {
        if(minimum != null && value < minimum) {
            return false;
        }

        return maximim == null || !(value > maximim);
    }

    private boolean transactionPassFilterValue(TransactionReportDTO transaction, TransactionFilterDTO filter) {
        if(filter.getValueRange() == null) {
            return true;
        }

        // First check the sign.
        if( (transaction.getAmount().isNegative() && !filter.getValueRange().getDebit()) ||
            (!transaction.getAmount().isNegative() && filter.getValueRange().getDebit()) ) {
            // Sign mismatch.
            return false;
        }

        // Check the value.
        double absValue = Math.abs(transaction.getAmount().getValue());
        if(absValue < filter.getValueRange().getMinimum()) {
            return false;
        }

        return !(absValue > filter.getValueRange().getMaximum());
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
        return filter.getCategories().isEmpty();
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
        }

        //TODO
        // Transaction does not have details of the statement!
        return true;
    }

    public Optional<TransactionReportDTO> passTransaction(Transaction transaction, TransactionFilterDTO filter) {
        TransactionReportDTO result = transactionMapper.map(transaction,TransactionReportDTO.class);

        // Make sure the transaction passes the filters.
        if(!transactionPassFilterValue(result, filter)) {
            return Optional.empty();
        }

        if(!transctionPassFilterDate(result, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterAccount(result, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterCategory(result, filter)) {
            return Optional.empty();
        }

        if(!transactionPassFilterStatement(result, filter)) {
            return Optional.empty();
        }

        return Optional.of(result);
    }
}
