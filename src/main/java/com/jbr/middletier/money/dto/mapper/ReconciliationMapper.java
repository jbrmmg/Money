package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.dto.mapper.converter.LocalDateStringConverter;
import com.jbr.middletier.money.dto.mapper.converter.MatchDataToDTO;
import com.jbr.middletier.money.dto.mapper.converter.TransactionToDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class ReconciliationMapper extends ModelMapper {
    public ReconciliationMapper() {
        LocalDateStringConverter dateConverter = new LocalDateStringConverter();
        this.addConverter(new MatchDataToDTO(new TransactionToDTO(dateConverter),dateConverter));
    }
}
