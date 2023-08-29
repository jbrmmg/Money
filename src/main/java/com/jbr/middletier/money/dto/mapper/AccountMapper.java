package com.jbr.middletier.money.dto.mapper;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.dto.AccountDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class AccountMapper extends ModelMapper {
    public AccountMapper() {
        this.createTypeMap(Account.class, AccountDTO.class);
        this.createTypeMap(AccountDTO.class, Account.class);
    }
}
