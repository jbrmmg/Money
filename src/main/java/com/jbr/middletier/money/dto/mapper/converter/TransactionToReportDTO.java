package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import org.modelmapper.AbstractConverter;

public class TransactionToReportDTO extends AbstractConverter<Transaction, TransactionReportDTO> {
    private final LocalDateStringConverter localDateStringConverter;
    private final AccountManager accountManager;
    private final CategoryManager categoryManager;
    private final StatementManager statementManager;

    public TransactionToReportDTO(LocalDateStringConverter localDateStringConverter,
                                  AccountManager accountManager,
                                  CategoryManager categoryManager,
                                  StatementManager statementManager) {
        this.localDateStringConverter = localDateStringConverter;
        this.accountManager = accountManager;
        this.categoryManager = categoryManager;
        this.statementManager = statementManager;
    }

    @Override
    protected TransactionReportDTO convert(Transaction transaction) {
        if(transaction == null)
            return null;

        TransactionReportDTO result = new TransactionReportDTO();

        result.setTransactionId(transaction.getId());
        result.setDate(localDateStringConverter.convert(transaction.getDate()));
        result.setDescription(transaction.getDescription());
        result.setFromReconciliation(false);
        result.setPredicted(false);
        if(transaction.getAccount() != null) {
            result.setAccount(this.accountManager.getExternalIfValid(transaction.getAccount().getId()).orElse(null));
        }
        if(transaction.getStatement() != null && transaction.getAccount() != null) {
            result.setStatement(this.statementManager.getStatementExternal(transaction.getAccount(),
                    transaction.getStatement().getId().getMonth(),
                    transaction.getStatement().getId().getYear()).orElse(null));
        } else {
            result.setStatement(null);
            result.setActionReconcile(true);
            result.setActionDelete(true);
        }
        result.setAmount(transaction.getAmount());
        if(transaction.getCategory() != null) {
            result.setCategory(this.categoryManager.getExternalIfValid(transaction.getCategory().getId()).orElse(null));
        }
        result.setOppositeId(transaction.getOppositeTransactionId());
        result.setActionUpdate(true);

        return result;
    }
}
