package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.TransactionDTO;
import org.modelmapper.AbstractConverter;

public class TransactionToDTO extends AbstractConverter<Transaction, TransactionDTO> {
    private final LocalDateStringConverter localDateStringConverter;

    public TransactionToDTO(LocalDateStringConverter localDateStringConverter) {
        this.localDateStringConverter = localDateStringConverter;
    }

    @Override
    protected TransactionDTO convert(Transaction source) {
        if(source == null)
            return null;

        TransactionDTO result = new TransactionDTO();

        result.setDate(this.localDateStringConverter.convert(source.getDate()));
        result.setDescription(source.getDescription());
        result.setStatementLocked(source.getStatement() != null && source.getStatement().getLocked());
        result.setCategoryId(source.getCategory() == null ? null : source.getCategory().getId());
        result.setAccountId(source.getAccount() == null ? null : source.getAccount().getId());
        result.setAmount(source.getAmount().getValue());
        result.setStatementYear(source.getStatement() == null ? null : source.getStatement().getId().getYear());
        result.setStatementMonth(source.getStatement() == null ? null : source.getStatement().getId().getMonth());
        result.setHasStatement(source.getStatement() != null);
        result.setId(source.getId());
        result.setOppositeTransactionId(source.getOppositeTransactionId());

        return result;
    }
}
