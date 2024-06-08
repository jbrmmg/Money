package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.data.primary.Transaction;
import org.modelmapper.AbstractConverter;

public class TransactionToReport extends AbstractConverter<Transaction, TransactionReport> {
    private final LocalDateStringConverter localDateStringConverter;

    public TransactionToReport(LocalDateStringConverter localDateStringConverter) {
        this.localDateStringConverter = localDateStringConverter;
    }

    @Override
    protected TransactionReport convert(Transaction transaction) {
        if(transaction == null)
            return null;

        TransactionReport result = new TransactionReport();

        result.setTransactionId(transaction.getId());
        if(transaction.getDate() != null) {
            result.setDate(this.localDateStringConverter.convert(transaction.getDate()));
        }
        result.setDescription(transaction.getDescription());
        result.setSearchDescription(transaction.getDescription().toLowerCase().replaceAll("[^a-z0-9]",""));
        result.setFromReconciliation(false);
        result.setPredicted(false);
        if(transaction.getAccount() != null) {
            result.setAccountId(transaction.getAccount().getId());
        }
        if(transaction.getStatement() != null) {
            result.setStatementYear(transaction.getStatement().getId().getYear());
            result.setStatementMonth(transaction.getStatement().getId().getMonth());
            result.setStatementSort(transaction.getStatement().getId().getYear() * 100 + transaction.getStatement().getId().getMonth());
            result.setStatementOpenBalance(transaction.getStatement().getOpenBalance().getValue());
            result.setLocked(transaction.getStatement().getLocked());
            if(!transaction.getStatement().getLocked()) {
                result.setActionUnreconcile(true);
            }
        } else {
            result.setStatementSort(999999);
            result.setActionReconcile(true);
            result.setActionDelete(true);
            result.setActionUpdate(true);
            result.setLocked(false);
        }
        result.setAmount(transaction.getAmount().getValue());
        if(transaction.getCategory() != null) {
            result.setCategoryId(transaction.getCategory().getId());
        }
        result.setOppositeId(transaction.getOppositeTransactionId());
        result.setActionUpdateCategory(true);

        return result;
    }
}
