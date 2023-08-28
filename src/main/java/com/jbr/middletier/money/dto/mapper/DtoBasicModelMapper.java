package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.StatementId;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class DtoBasicModelMapper extends ModelMapper {
    // Perform basic model mapping - very simple data transfers.
    public DtoBasicModelMapper() {
        this.createTypeMap(Account.class, AccountDTO.class);
        this.createTypeMap(AccountDTO.class, Account.class);
        this.createTypeMap(Category.class, CategoryDTO.class);
        this.createTypeMap(CategoryDTO.class, Category.class);
    }
}
