package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.mapper.AccountMapper;
import com.jbr.middletier.money.exceptions.CreateAccountException;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Collections;
import java.util.List;

@Controller
public class AccountManager extends AbstractManager<
        Account,
        AccountDTO,
        String,
        AccountRepository,
        CreateAccountException,
        UpdateDeleteAccountException> {
    @Autowired
    public AccountManager(AccountMapper modelMapper, AccountRepository accountRepository)  {
        super(AccountDTO.class,Account.class,modelMapper,accountRepository);
    }

    @Override
    String getInstanceId(Account instance) {
        return instance.getId();
    }

    @Override
    CreateAccountException getAddException(String id) {
        return new CreateAccountException(id);
    }

    @Override
    UpdateDeleteAccountException getUpdateDeleteException(String id) {
        return new UpdateDeleteAccountException(id);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    void validateUpdateOrDelete(Account instance, boolean update) throws UpdateDeleteAccountException {
        // Not required for accounts.
    }

    @Override
    void updateInstance(Account instance, Account from) {
        instance.setColour(from.getColour());
        instance.setImagePrefix(from.getImagePrefix());
        instance.setName(from.getName());
    }
}
