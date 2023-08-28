package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Account;
import org.modelmapper.AbstractConverter;

public class AccountStringConverter extends AbstractConverter<Account,String> {
    @Override
    protected String convert(Account account) {
        return account.getId();
    }
}
