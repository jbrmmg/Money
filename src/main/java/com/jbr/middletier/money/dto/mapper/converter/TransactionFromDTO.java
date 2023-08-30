package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import org.modelmapper.AbstractConverter;

public class TransactionFromDTO extends AbstractConverter<TransactionDTO, Transaction> {
    private final AccountManager accountManager;
    private final CategoryManager categoryManager;
    private final StatementManager statementManager;
    private final StringLocalDateConverter stringLocalDateConverter;

    public TransactionFromDTO(AccountManager accountManager,
                              CategoryManager categoryManager,
                              StatementManager statementManager,
                              StringLocalDateConverter stringLocalDateConverter) {
        this.accountManager = accountManager;
        this.categoryManager = categoryManager;
        this.statementManager = statementManager;
        this.stringLocalDateConverter = stringLocalDateConverter;
    }

    @Override
    protected Transaction convert(TransactionDTO source) {
        Transaction result = new Transaction();

        result.setAccount(accountManager.getIfValid(source.getAccountId()).orElse(null));
        result.setAmount(source.getAmount());
        result.setCategory(categoryManager.getIfValid(source.getCategoryId()).orElse(null));
        result.setDescription(source.getDescription());
        result.setOppositeTransactionId(source.getOppositeTransactionId());
        result.setDate(this.stringLocalDateConverter.convert(source.getDate()));

        // Load the statement, if specified.
        result.setStatement(statementManager.getStatement(accountManager.getIfValid(source.getAccountId()).orElse(null),
                source.getStatementMonth(),
                source.getStatementYear()).orElse(null));

        return result;
    }
}
