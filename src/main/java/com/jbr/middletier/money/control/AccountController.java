package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.exceptions.AccountAlreadyExistsException;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
import com.jbr.middletier.money.manager.LogoManager;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */

@Controller
@RequestMapping("/jbr")
public class AccountController {
    private static final Logger LOG = LoggerFactory.getLogger(AccountController.class);

    private final AccountRepository accountRepository;
    private final LogoManager logoManager;
    private final ModelMapper modelMapper;

    @Autowired
    public AccountController(AccountRepository accountRepository, LogoManager logoManager, ModelMapper modelMapper) {
        this.accountRepository = accountRepository;
        this.logoManager = logoManager;
        this.modelMapper = modelMapper;
    }

    public String getAccountLogo(String id, Boolean disabled) {
        return this.logoManager.getSvgLogoForAccount(id,disabled).getSvgAsString();
    }

    @GetMapping(path="/int/money/account/logo")
    public @ResponseBody ResponseEntity<String> getIntAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (int)");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(getAccountLogo(id, disabled), headers, HttpStatus.OK);
    }

    @GetMapping(path="/ext/money/account/logo")
    public @ResponseBody ResponseEntity<String> getExtAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (ext)");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(getAccountLogo(id, disabled), headers, HttpStatus.OK);
    }

    private List<AccountDTO> getAccounts() {
        List<AccountDTO> result = new ArrayList<>();
        for(Account nextAccount: accountRepository.findAll()) {
            result.add(this.modelMapper.map(nextAccount, AccountDTO.class));
        }

        return result;
    }

    @GetMapping(path="/ext/money/accounts")
    public @ResponseBody List<AccountDTO> getExtAccounts() {
        LOG.info("Request Accounts (ext).");

        return getAccounts();
    }

    @GetMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO>  getIntAccounts() {
        LOG.info("Request Accounts (int).");

        return getAccounts();
    }

    @PostMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> createAccount(@RequestBody AccountDTO account) throws AccountAlreadyExistsException {
        LOG.info("Create a new account - {}}", account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            throw new AccountAlreadyExistsException(account);
        }

        accountRepository.save(this.modelMapper.map(account,Account.class));

        return getAccounts();
    }

    @PutMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> updateAccount(@RequestBody AccountDTO account) throws InvalidAccountIdException {
        LOG.info("Update an account - {}", account.getId());

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

    @DeleteMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> deleteAccount(@RequestBody AccountDTO account) throws InvalidAccountIdException {
        LOG.info("Delete account {}", account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            accountRepository.delete(existingAccount.get());

            return getAccounts();
        }

        throw new InvalidAccountIdException(account);
    }
}
