package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

import java.math.BigDecimal;

public class FinancialAmountDoubleConverter extends AbstractConverter<FinancialAmount, BigDecimal> {
    @Override
    protected BigDecimal convert(FinancialAmount financialAmount) {
        return financialAmount.getValue();
    }
}
