package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.OkStatus;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
import com.jbr.middletier.money.manager.LogoManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final AccountRepository accountRepository;
    private final LogoManager logoManager;

    @Autowired
    public AccountController(AccountRepository accountRepository, LogoManager logoManager) {
        this.accountRepository = accountRepository;
        this.logoManager = logoManager;
    }

    public String getAccountLogo(String id, Boolean disabled) {
        return this.logoManager.getSvgLogoForAccount(id,disabled).getSvgAsString();
    }

    @ExceptionHandler(Exception.class)
    public void handleException(HttpServletResponse response) throws IOException {
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

    @RequestMapping(path="/int/money/account/logo", method= RequestMethod.GET)
    public @ResponseBody ResponseEntity<String> getIntAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (int)");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(getAccountLogo(id, disabled), headers, HttpStatus.OK);
    }

    @RequestMapping(path="/ext/money/account/logo", method= RequestMethod.GET)
    public @ResponseBody ResponseEntity<String> getExtAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (ext)");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(getAccountLogo(id, disabled), headers, HttpStatus.OK);
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
    public @ResponseBody OkStatus deleteAccount(@RequestBody Account account) throws InvalidAccountIdException {
        LOG.info("Delete account " + account.getId());

        // Is there an account with this ID?
        Optional<Account> existingAccount = accountRepository.findById(account.getId());
        if(existingAccount.isPresent()) {
            accountRepository.delete(existingAccount.get());
            return OkStatus.getOkStatus();
        }

        throw new InvalidAccountIdException(account);
    }
}
