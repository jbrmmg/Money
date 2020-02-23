package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */

@Controller
@RequestMapping("/jbr")
public class AccountController {
    final static private Logger LOG = LoggerFactory.getLogger(AccountController.class);

    private final
    AccountRepository accountRepository;

    @Autowired
    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    @RequestMapping(path="/ext/money/accounts", method= RequestMethod.GET)
    public @ResponseBody Iterable<Account>  getExtAccounts() {
        LOG.info("Request Accounts (ext).");
        return accountRepository.findAll();
    }

    @RequestMapping(path="/int/money/accounts", method= RequestMethod.GET)
    public @ResponseBody Iterable<Account>  getIntAccounts() {
        LOG.info("Request Accounts (int).");
        return accountRepository.findAll();
    }

    @RequestMapping(path="/int/money/accounts",method=RequestMethod.POST)
    public @ResponseBody Iterable<Account> createAccount(@RequestBody Account account) throws Exception {
        LOG.info("Create a new account - " + account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            throw new Exception(account.getId() + " already exists");
        }

        accountRepository.save(account);

        return accountRepository.findAll();
    }

    @RequestMapping(path="/int/money/accounts",method=RequestMethod.PUT)
    public @ResponseBody Iterable<Account> updateAccount(@RequestBody Account account) {
        LOG.info("Update an account - " + account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            existingAccount.get().setColor(account.getColour());
            existingAccount.get().setImagePrefix(account.getImagePrefix());
            existingAccount.get().setColor(account.getColour());
            existingAccount.get().setName(account.getName());
            accountRepository.save(existingAccount.get());
        }

        return accountRepository.findAll();
    }

    @RequestMapping(path="/int/money/accounts",method=RequestMethod.DELETE)
    public @ResponseBody StatusResponse deleteAccount(@RequestBody Account account) {
        LOG.info("Delete account " + account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            accountRepository.delete(existingAccount.get());
            return new StatusResponse();
        }

        return new StatusResponse("Account does not exist " + account.getId());
    }
}
