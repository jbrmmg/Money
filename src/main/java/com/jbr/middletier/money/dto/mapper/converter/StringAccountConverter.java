package com.jbr.middletier.money.dto.mapper.converter;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.manager.AccountManager;
import org.modelmapper.AbstractConverter;

public class StringAccountConverter extends AbstractConverter<String, Account> {
    AccountManager accountManager;

    public StringAccountConverter(AccountManager accountManager) {
        this.accountManager = accountManager;
    }

    @Override
    protected Account convert(String s) {
        return accountManager.findAccountById(s);
    }
}
