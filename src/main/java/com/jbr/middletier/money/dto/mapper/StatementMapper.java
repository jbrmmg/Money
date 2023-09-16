package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.dto.mapper.converter.DoubleFinancialAmountConverter;
import com.jbr.middletier.money.dto.mapper.converter.FinancialAmountDoubleConverter;
import com.jbr.middletier.money.dto.mapper.converter.StatementFromDTO;
import com.jbr.middletier.money.manager.AccountManager;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.modelmapper.ModelMapper;

@Controller
public class StatementMapper extends ModelMapper {
    @Autowired
    public StatementMapper(AccountManager accountManager) {
        this.addConverter(new FinancialAmountDoubleConverter());
        this.addConverter(new DoubleFinancialAmountConverter());
        this.addConverter(new StatementFromDTO(accountManager));

        // Statement mapper.
        TypeMap<Statement, StatementDTO> statementMapper = this.createTypeMap(Statement.class, StatementDTO.class);
        statementMapper.addMappings(
                mapper -> mapper.map(src -> src.getId().getYear(), StatementDTO::setYear)
        );
        statementMapper.addMappings(
                mapper -> mapper.map(src -> src.getId().getMonth(), StatementDTO::setMonth)
        );
        this.createTypeMap(StatementId.class, StatementIdDTO.class);
        this.createTypeMap(StatementIdDTO.class, StatementId.class);
    }
}
