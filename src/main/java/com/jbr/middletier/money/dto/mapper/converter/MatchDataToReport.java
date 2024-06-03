package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionReportDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import com.jbr.middletier.money.dto.mapper.CategoryMapper;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

public class MatchDataToReport extends AbstractConverter<MatchData, TransactionReport> {
    private final LocalDateStringConverter localDateStringConverter;

    public MatchDataToReport(LocalDateStringConverter localDateStringConverter) {
        this.localDateStringConverter = localDateStringConverter;
    }

    @Override
    protected TransactionReport convert(MatchData matchData) {
        if(matchData == null)
            return null;

        TransactionReport result = new TransactionReport();

        if(matchData.getTransaction() != null) {
            result.setId(matchData.getTransaction().getId());
        }
        if(matchData.getDate() != null) {
            result.setDate(this.localDateStringConverter.convert(matchData.getDate()));
        }
        result.setDescription(matchData.getDescription());
        result.setSearchDescription(matchData.getDescription().toLowerCase().replaceAll("[^a-z0-9]",""));
        result.setFromReconciliation(true);
        result.setPredicted(false);
        if(matchData.getAccount() != null) {
            result.setAccountId(matchData.getAccount().getId());
        }
        result.setAmount(matchData.getAmount());
        if(matchData.getCategory() != null) {
            result.setCategoryId(matchData.getCategory().getId());
        }
        result.setLocked(false);
        result.setActionUpdateCategory(true);

        return result;
    }
}
