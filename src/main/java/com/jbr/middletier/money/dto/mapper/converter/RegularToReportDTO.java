package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import com.jbr.middletier.money.dto.mapper.CategoryMapper;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

import java.time.LocalDate;

public class RegularToReportDTO extends AbstractConverter<Regular, TransactionReportDTO> {
    private final LocalDateStringConverter localDateStringConverter;
    private final AccountMapper accountMapper;
    private final CategoryMapper categoryMapper;
    private final ApplicationProperties applicationProperties;

    public RegularToReportDTO(ApplicationProperties applicationProperties, LocalDateStringConverter localDateStringConverter, AccountMapper accountMapper, CategoryMapper categoryMapper) {
        this.localDateStringConverter = localDateStringConverter;
        this.accountMapper = accountMapper;
        this.categoryMapper = categoryMapper;
        this.applicationProperties = applicationProperties;
    }

    private LocalDate determineDate(Regular source) {
        try {
            return source.getNextDate(this.applicationProperties.getToday());
        } catch (CannotDetermineNextDateException e) {
            return null;
        }
    }

    @Override
    protected TransactionReportDTO convert(Regular source) {
        if(source == null)
            return null;

        TransactionReportDTO result = new TransactionReportDTO();

        LocalDate nextDate = determineDate(source);
        if(nextDate != null) {
            result.setDate(this.localDateStringConverter.convert(nextDate));
        }
        result.setDescription(source.getDescription());
        result.setFromReconciliation(false);
        result.setPredicted(true);
        if(source.getAccount() != null) {
            result.setAccount(this.accountMapper.map(source.getAccount(), AccountDTO.class));
        }
        result.setAmount(new FinancialAmount(source.getAmount()));
        if(source.getCategory() != null) {
            result.setCategory(this.categoryMapper.map(source.getCategory(), CategoryDTO.class));
        }
        result.setBalance(new FinancialAmount(0));

        return result;
    }
}
