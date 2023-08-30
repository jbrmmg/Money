package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

public class FinancialAmountDoubleConverter extends AbstractConverter<FinancialAmount,Double> {
    @Override
    protected Double convert(FinancialAmount financialAmount) {
        return financialAmount.getValue();
    }
}
