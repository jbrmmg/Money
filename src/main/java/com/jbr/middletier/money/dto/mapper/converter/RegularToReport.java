package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import org.modelmapper.AbstractConverter;

import java.time.LocalDate;

public class RegularToReport extends AbstractConverter<Regular, TransactionReport> {
    private final ApplicationProperties applicationProperties;
    private final LocalDateStringConverter localDateStringConverter;

    public RegularToReport(ApplicationProperties applicationProperties, LocalDateStringConverter localDateStringConverter) {
        this.localDateStringConverter = localDateStringConverter;
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
    protected TransactionReport convert(Regular regular) {
        if(regular == null)
            return null;

        TransactionReport result = new TransactionReport();

        LocalDate nextDate = determineDate(regular);
        if(nextDate != null) {
            result.setDate(this.localDateStringConverter.convert(nextDate));
        }
        result.setDescription(regular.getDescription());
        if(regular.getDescription() != null) {
            result.setSearchDescription(regular.getDescription().toLowerCase().replaceAll("[^a-z0-9]", ""));
        }
        result.setFromReconciliation(false);
        result.setPredicted(true);
        if(regular.getAccount() != null) {
            result.setAccountId(regular.getAccount().getId());
        }
        result.setAmount(regular.getAmount());
        if(regular.getCategory() != null) {
            result.setCategoryId(regular.getCategory().getId());
        }
        result.setStatementSort(999999);
        result.setLocked(false);
        result.setActionUpdateCategory(true);

        return result;
    }
}
