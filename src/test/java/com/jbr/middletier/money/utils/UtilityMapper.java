package com.jbr.middletier.money.utils;

import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.mapper.converter.DoubleFinancialAmountConverter;
import com.jbr.middletier.money.dto.mapper.converter.FinancialAmountDoubleConverter;
import com.jbr.middletier.money.dto.mapper.converter.LocalDateStringConverter;
import com.jbr.middletier.money.dto.mapper.converter.StringLocalDateConverter;
import com.jbr.middletier.money.util.DateRange;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UtilityMapper extends ModelMapper {
    public UtilityMapper() {
        this.addConverter(new FinancialAmountDoubleConverter());
        this.addConverter(new DoubleFinancialAmountConverter());
        this.addConverter(new LocalDateStringConverter());
        this.addConverter(new StringLocalDateConverter());

        this.createTypeMap(DateRange.class, DateRangeDTO.class);
        this.createTypeMap(DateRangeDTO.class, DateRange.class);
    }
}
