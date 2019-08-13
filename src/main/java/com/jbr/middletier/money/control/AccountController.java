package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

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
}
