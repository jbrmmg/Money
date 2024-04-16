package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Transaction;
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
        // If debit, check the debit filter.
        if(transaction.getAmount().isNegative()) {
            // Is there a debit restriction?
            if(filter.getDebitRange() != null) {
                return checkValueRange(transaction.getAmount().getValue() * -1,
                        filter.getDebitRange().getMinimum(),
                        filter.getDebitRange().getMaximum());
            }

            return true;
        }

        // If credit then check the credit filter.
        if(filter.getCreditRange() != null) {
            return checkValueRange(transaction.getAmount().getValue(),
                    filter.getCreditRange().getMinimum(),
                    filter.getCreditRange().getMaximum());
        }

        return true;
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
        return filter.getAccounts().isEmpty();
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
        result.setId(transaction.getId());
        result.setPredicted(false);
        result.setFromReconciliation(false);

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
