package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

public class DoubleFinancialAmountConverter extends AbstractConverter<Double, FinancialAmount> {
    @Override
    protected FinancialAmount convert(Double value) {
        return new FinancialAmount(value);
    }
}
