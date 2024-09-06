package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.mapper.converter.*;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.DateRange;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class TransactionMapper extends ModelMapper {
    @Autowired
    public TransactionMapper(AccountManager accountManager,
                             CategoryManager categoryManager,
                             StatementManager statementManager,
                             ApplicationProperties applicationProperties) {
        StringLocalDateConverter stringLocalDateConverter = new StringLocalDateConverter();
        LocalDateStringConverter localDateStringConverter = new LocalDateStringConverter();
        this.addConverter(new AccountStringConverter());
        this.addConverter(new StringAccountConverter(accountManager));
        this.addConverter(new CategoryStringConverter());
        this.addConverter(new StringCategoryConverter(categoryManager));
        this.addConverter(localDateStringConverter);
        this.addConverter(stringLocalDateConverter);
        this.addConverter(new FinancialAmountDoubleConverter());
        this.addConverter(new DoubleFinancialAmountConverter());
        this.addConverter(new TransactionFromDTO(accountManager,categoryManager,statementManager,stringLocalDateConverter));
        this.addConverter(new TransactionToDTO(localDateStringConverter));
        this.addConverter(new ReconciliationFileToDTO(accountManager));
        this.addConverter(new TransactionToReport(localDateStringConverter));
        this.addConverter(new TransactionDTOToReportDTO(accountManager,categoryManager,statementManager));
        this.addConverter(new TransactionToReportDTO(localDateStringConverter,accountManager,categoryManager,statementManager));
        this.addConverter(new RegularToReport(applicationProperties,localDateStringConverter));
        this.addConverter(new MatchDataToReport(localDateStringConverter));
        this.addConverter(new TransactionReportToDTO(accountManager,categoryManager,statementManager));
        this.createTypeMap(DateRange.class, DateRangeDTO.class);
        this.createTypeMap(DateRangeDTO.class, DateRange.class);
    }
}
