package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.dto.mapper.converter.*;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class RegularMapper extends ModelMapper {
    public RegularMapper(AccountManager accountManager, CategoryManager categoryManager) {
        this.addConverter(new AccountStringConverter());
        this.addConverter(new StringAccountConverter(accountManager));
        this.addConverter(new CategoryStringConverter());
        this.addConverter(new StringCategoryConverter(categoryManager));
        this.addConverter(new AdjustmentTypeStringConverter());
        this.addConverter(new StringAdjustmentTypeConverter());
        this.addConverter(new LocalDateStringConverter());
        this.addConverter(new StringLocalDateConverter());
        this.createTypeMap(Regular.class, RegularDTO.class);
        this.createTypeMap(RegularDTO.class, Regular.class);
    }
}
