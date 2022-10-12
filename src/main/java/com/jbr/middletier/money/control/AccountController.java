package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.exceptions.AccountAlreadyExistsException;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
import com.jbr.middletier.money.manager.AccountManager;
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
import java.util.List;

/**
 * Created by jason on 07/03/17.
 */

@Controller
@RequestMapping("/jbr")
public class AccountController {
    private static final Logger LOG = LoggerFactory.getLogger(AccountController.class);


    private final LogoManager logoManager;

    private final AccountManager accountManager;

    @Autowired
    public AccountController(LogoManager logoManager, AccountManager accountManager) {
        this.logoManager = logoManager;
        this.accountManager = accountManager;
    }

    @GetMapping(path="/int/money/account/logo")
    public @ResponseBody ResponseEntity<String> getIntAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (int)");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(this.logoManager.getSvgLogoForAccount(id,disabled).getSvgAsString(), headers, HttpStatus.OK);
    }

    @GetMapping(path="/ext/money/account/logo")
    public @ResponseBody ResponseEntity<String> getExtAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (ext)");
        return getIntAccountLogo(id, disabled);
    }

    @GetMapping(path="/ext/money/accounts")
    public @ResponseBody List<AccountDTO> getExtAccounts() {
        LOG.info("Request Accounts (ext).");

        return accountManager.getAccounts();
    }

    @GetMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO>  getIntAccounts() {
        LOG.info("Request Accounts (int).");

        return accountManager.getAccounts();
    }

    @PostMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> createAccount(@RequestBody AccountDTO account) throws AccountAlreadyExistsException {
        LOG.info("Create a new account - {}}", account.getId());
        return accountManager.createAccount(account);
    }

    @PutMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> updateAccount(@RequestBody AccountDTO account) throws InvalidAccountIdException {
        LOG.info("Update an account - {}", account.getId());
        return accountManager.updateAccount(account);
    }

    @DeleteMapping(path="/int/money/accounts")
    public @ResponseBody List<AccountDTO> deleteAccount(@RequestBody AccountDTO account) throws InvalidAccountIdException {
        LOG.info("Delete account {}", account.getId());
        return accountManager.deleteAccount(account);
    }
}
