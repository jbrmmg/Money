package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.mapper.DtoBasicModelMapper;
import com.jbr.middletier.money.exceptions.AccountAlreadyExistsException;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountManager {
    private final DtoBasicModelMapper modelMapper;
    private final AccountRepository accountRepository;

    @Autowired
    public AccountManager(DtoBasicModelMapper modelMapper, AccountRepository accountRepository) {
        this.modelMapper = modelMapper;
        this.accountRepository = accountRepository;
    }

    public List<AccountDTO> getAccounts() {
        List<AccountDTO> result = new ArrayList<>();
        for(Account nextAccount: accountRepository.findAll()) {
            result.add(this.modelMapper.map(nextAccount, AccountDTO.class));
        }

        Collections.sort(result);

        return result;
    }

    public Account findAccountById(String id) {
        return accountRepository.findById(id).orElse(null);
    }

    public List<AccountDTO> createAccount(AccountDTO account) throws AccountAlreadyExistsException {
        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            throw new AccountAlreadyExistsException(account);
        }

        accountRepository.save(this.modelMapper.map(account,Account.class));

        return getAccounts();
    }

    public List<AccountDTO> updateAccount(AccountDTO account) throws InvalidAccountIdException {

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            existingAccount.get().setColour(account.getColour());
            existingAccount.get().setImagePrefix(account.getImagePrefix());
            existingAccount.get().setName(account.getName());
            accountRepository.save(existingAccount.get());

            return getAccounts();
        }

        throw new InvalidAccountIdException(account);
    }

    public List<AccountDTO> deleteAccount(AccountDTO account) throws InvalidAccountIdException {

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            accountRepository.delete(existingAccount.get());

            return getAccounts();
        }

        throw new InvalidAccountIdException(account);
    }
}
