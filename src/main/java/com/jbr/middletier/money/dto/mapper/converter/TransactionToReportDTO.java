package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import com.jbr.middletier.money.dto.mapper.CategoryMapper;
import com.jbr.middletier.money.dto.mapper.StatementMapper;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

public class TransactionToReportDTO extends AbstractConverter<Transaction, TransactionReportDTO> {
    private final LocalDateStringConverter localDateStringConverter;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final StatementMapper statementMapper;

    public TransactionToReportDTO(LocalDateStringConverter localDateStringConverter, AccountMapper accountMapper, CategoryMapper categoryMapper, StatementMapper statementMapper) {
        this.localDateStringConverter = localDateStringConverter;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.statementMapper = statementMapper;
    }

    @Override
    protected TransactionReportDTO convert(Transaction source) {
        if(source == null)
            return null;

        TransactionReportDTO result = new TransactionReportDTO();

        if(source.getDate() != null) {
            result.setDate(this.localDateStringConverter.convert(source.getDate()));
        }
        result.setDescription(source.getDescription());
        result.setId(source.getId());
        result.setFromReconciliation(false);
        result.setPredicted(false);
        if(source.getAccount() != null) {
            result.setAccount(this.accountMapper.map(source.getAccount(), AccountDTO.class));
        }
        if(source.getStatement() != null) {
            result.setStatement(this.statementMapper.map(source.getStatement(), StatementDTO.class));
        }
        result.setAmount(source.getAmount());
        if(source.getCategory() != null) {
            result.setCategory(this.categoryMapper.map(source.getCategory(), CategoryDTO.class));
        }
        result.setBalance(new FinancialAmount(0));
        result.setOppositeId(source.getOppositeTransactionId());

        return result;
    }
}
