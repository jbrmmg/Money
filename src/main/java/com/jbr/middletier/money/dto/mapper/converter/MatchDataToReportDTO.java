package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import com.jbr.middletier.money.dto.mapper.CategoryMapper;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

public class MatchDataToReportDTO  extends AbstractConverter<MatchData, TransactionReportDTO> {
    private final LocalDateStringConverter localDateStringConverter;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;

    public MatchDataToReportDTO(LocalDateStringConverter localDateStringConverter, AccountMapper accountMapper, CategoryMapper categoryMapper) {
        this.localDateStringConverter = localDateStringConverter;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    protected TransactionReportDTO convert(MatchData matchData) {
        if(matchData == null)
            return null;

        TransactionReportDTO result = new TransactionReportDTO();

        if(matchData.getTransaction() != null) {
            result.setId(matchData.getTransaction().getId());
        }
        if(matchData.getDate() != null) {
            result.setDate(this.localDateStringConverter.convert(matchData.getDate()));
        }
        result.setDescription(matchData.getDescription());
        result.setFromReconciliation(true);
        result.setPredicted(false);
        if(matchData.getAccount() != null) {
            result.setAccount(this.accountMapper.map(matchData.getAccount(), AccountDTO.class));
        }
        result.setAmount(new FinancialAmount(matchData.getAmount()));
        if(matchData.getCategory() != null) {
            result.setCategory(this.categoryMapper.map(matchData.getCategory(), CategoryDTO.class));
        }
        result.setBalance(new FinancialAmount(0));

        return result;
    }
}
