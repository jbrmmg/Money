package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.AbstractConverter;

import java.math.BigDecimal;

public class DoubleFinancialAmountConverter extends AbstractConverter<BigDecimal, FinancialAmount> {
    @Override
    protected FinancialAmount convert(BigDecimal value) {
        if(value == null) {
            return new FinancialAmount();
        }
        return new FinancialAmount(value);
    }
}
