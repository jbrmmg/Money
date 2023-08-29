package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.dto.mapper.converter.DoubleFinancialAmountConverter;
import com.jbr.middletier.money.dto.mapper.converter.FinancialAmountDoubleConverter;
import com.jbr.middletier.money.dto.mapper.converter.LocalDateStringConverter;
import com.jbr.middletier.money.dto.mapper.converter.StringLocalDateConverter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class UtilityMapper extends ModelMapper {
    public UtilityMapper() {
        this.addConverter(new FinancialAmountDoubleConverter());
        this.addConverter(new DoubleFinancialAmountConverter());
        this.addConverter(new LocalDateStringConverter());
        this.addConverter(new StringLocalDateConverter());
    }
}
