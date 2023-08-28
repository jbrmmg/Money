package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.converter.*;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import org.modelmapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.time.LocalDate;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

@Controller
public class DtoComplexModelMapper extends ModelMapper {

    // Perform more complex model mapping - where it's not simple.
    @Autowired
    public DtoComplexModelMapper(AccountManager accountManager, CategoryManager categoryManager) {
        this.addConverter(new FinancialAmountDoubleConverter());
        this.addConverter(new DoubleFinancialAmountConverter());
        this.addConverter(new AccountStringConverter());
        this.addConverter(new StringAccountConverter(accountManager));
        this.addConverter(new CategoryStringConverter());
        this.addConverter(new StringCategoryConverter(categoryManager));
        this.addConverter(new LocalDateStringConverter());
        this.addConverter(new StringLocalDateConverter());
        this.addConverter(new AdjustmentTypeStringConverter());
        this.addConverter(new StringAdjustmentTypeConverter());

        this.createTypeMap(Statement.class, StatementDTO.class);
        this.createTypeMap(StatementDTO.class, Statement.class);
        this.createTypeMap(Transaction.class, TransactionDTO.class);
        this.createTypeMap(TransactionDTO.class, Transaction.class);
        this.createTypeMap(Regular.class,RegularDTO.class);
        this.createTypeMap(RegularDTO.class, Regular.class);
        this.createTypeMap(StatementId.class, StatementIdDTO.class);
        this.createTypeMap(StatementIdDTO.class, StatementId.class);
    }
}
