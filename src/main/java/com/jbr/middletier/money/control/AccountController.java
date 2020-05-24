package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.OkStatus;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.exceptions.InvalidAccountIdException;
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

    public String getAccountLogo(String id, Boolean disabled) {
        // Generate a logo, SVG
        StringBuilder stringBuilder = new StringBuilder();

        int fontSize = 32;
        int y = 62;
        String fillColour = "656565";
        String borderColour = "FFFFFF";
        String textColour = "FFFFFF";
        String text = "UNK";
        boolean secondBorder = false;
        String border2Colour = "";

        // TODO - move to database
        switch(id) {
            case "JLPC":
                fontSize = 75;
                y = 74;
                fillColour = "003B25";
                borderColour = disabled ? "5C5C5C" : "FFFFFF";
                textColour = disabled ? "5C5C5C" : "FFFFFF";
                text = "jl";
                break;
            case "BANK":
                fontSize = 75;
                y = 74;
                fillColour = "000000";
                borderColour = disabled ? "5C5C5C" : "FFFFFF";
                textColour = disabled ? "5C5C5C" : "FFFFFF";
                text = "fd";
                break;
            case "AMEX":
                fontSize = 56;
                y = 66;
                fillColour = "006FCF";
                borderColour = disabled ? "5C5C5C" : "FFFFFF";
                textColour = disabled ? "5C5C5C" : "FFFFFF";
                text = "AM";
                break;
            case "NWDE":
                fontSize = 48;
                y = 66;
                fillColour = "004A8F";
                borderColour = disabled ? "5C5C5C" : "FFFFFF";
                textColour = disabled ? "5C5C5C" : "FFFFFF";
                text = "NW";
                secondBorder = true;
                border2Colour = disabled ? "ACACAC" : "ED1C24";
                break;
        }

        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        stringBuilder.append("<svg\n");
        stringBuilder.append("        width   = \"100\"\n");
        stringBuilder.append("        height  = \"100\"\n");
        stringBuilder.append("        viewBox = \"0 0 100 100\"\n");
        stringBuilder.append("        xmlns   = \"http://www.w3.org/2000/svg\">\n");
        stringBuilder.append("    <style type=\"text/css\">\n");
        stringBuilder.append("        <![CDATA[\n");
        stringBuilder.append("         tspan.am {\n");
        stringBuilder.append("            font-weight: bold;\n");
        stringBuilder.append("            font-size:   ").append(fontSize).append("px;\n");
        stringBuilder.append("            line-height: 125%;\n");
        stringBuilder.append("            font-family: Arial;\n");
        stringBuilder.append("            text-align:  center;\n");
        stringBuilder.append("            text-anchor: middle;\n");
        stringBuilder.append("            fill:        #").append(textColour).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.am {\n");
        stringBuilder.append("            fill: #").append(fillColour).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.amborder {\n");
        stringBuilder.append("            fill: #").append(borderColour).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.amborder2 {\n");
        stringBuilder.append("            fill: #").append(border2Colour).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("      ]]>\n");
        stringBuilder.append("    </style>\n");
        stringBuilder.append("    <rect class=\"amborder\" width=\"100\" height=\"100\" x=\"0\" y=\"0\"/>\n");
        if(secondBorder) {
            stringBuilder.append("    <rect class=\"amborder2\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n");
            stringBuilder.append("    <rect class=\"am\" width=\"80\" height=\"80\" x=\"10\" y=\"10\"/>\n");
            stringBuilder.append("    <text>\n");
            stringBuilder.append("        <tspan class=\"am\" x=\"50\" y=\"").append(y).append("\">").append(text).append("</tspan>\n");
            stringBuilder.append("    </text>\n");
        } else {
            stringBuilder.append("    <rect class=\"am\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n");
            stringBuilder.append("    <text>\n");
            stringBuilder.append("        <tspan class=\"am\" x=\"50\" y=\"").append(y).append("\">").append(text).append("</tspan>\n");
            stringBuilder.append("    </text>\n");
        }
        stringBuilder.append("</svg>");

        return stringBuilder.toString();
    }

    @Autowired
    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
    public @ResponseBody String getIntAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (int)");
        return getAccountLogo(id,disabled);
    }

    @RequestMapping(path="/ext/money/account/logo", method= RequestMethod.GET)
    public @ResponseBody String getExtAccountLogo(@RequestParam(value="id", defaultValue="UNKN") String id,
                                                  @RequestParam(value="disabled", defaultValue="false") Boolean disabled) {
        LOG.info("Account Logo (ext)");
        return getAccountLogo(id,disabled);
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
