package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.exceptions.CreateAccountException;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.LogoManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Accounts", description = "Bank account management and logo retrieval")
@Validated
public class AccountController {
    private static final Logger LOG = LoggerFactory.getLogger(AccountController.class);

    private final LogoManager logoManager;
    private final AccountManager accountManager;

    @Autowired
    public AccountController(LogoManager logoManager, AccountManager accountManager) {
        this.logoManager = logoManager;
        this.accountManager = accountManager;
    }

    @Operation(summary = "Retrieve the SVG logo for a bank account")
    @GetMapping(path="/account/logo")
    public ResponseEntity<String> getAccountLogo(@RequestParam(value="id", defaultValue="UNKN") @Pattern(regexp="^[\\da-zA-Z]{4}$",message="Id must be a four letter code") String id,
                                                 @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("image/svg+xml"));
        return new ResponseEntity<>(this.logoManager.getSvgLogoForAccount(id,disabled).getSvgAsString(), headers, HttpStatus.OK);
    }

    @GetMapping(path="/accounts")
    public List<AccountDTO> getAccounts() {
        LOG.info("Request Accounts.");
        return accountManager.getAll();
    }

    @PostMapping(path="/accounts")
    public List<AccountDTO> createAccount(@Valid @RequestBody AccountDTO account) throws CreateAccountException {
        LOG.info("Create a new account - {}}", account.getId());
        return accountManager.create(account);
    }

    @PutMapping(path="/accounts")
    public List<AccountDTO> updateAccount(@Valid @RequestBody AccountDTO account) throws UpdateDeleteAccountException {
        LOG.info("Update an account - {}", account.getId());
        return accountManager.update(account);
    }

    @DeleteMapping(path="/accounts")
    public List<AccountDTO> deleteAccount(@Valid @RequestBody AccountDTO account) throws UpdateDeleteAccountException {
        LOG.info("Delete account {}", account.getId());
        return accountManager.delete(account);
    }
}
