package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.StatementManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class StatementController {
    private static final Logger LOG = LoggerFactory.getLogger(StatementController.class);

    private final StatementManager statementManager;
    private final AccountTransactionManager accountTransactionManager;

    @Autowired
    public StatementController(StatementManager statementManager,
                               AccountTransactionManager accountTransactionManager) {
        this.statementManager = statementManager;
        this.accountTransactionManager = accountTransactionManager;
    }

    @GetMapping(path="/ext/money/statement")
    public @ResponseBody Iterable<StatementDTO>  statementsExt(@RequestParam(value="accountId", required = false) String accountId,
                                                               @RequestParam(value="locked", required = false) Boolean locked) {
        return this.statementManager.getStatements(accountId,locked);
    }

    @GetMapping(path="/int/money/statement")
    public @ResponseBody Iterable<StatementDTO>  statementsInt(@RequestParam(value="accountId", required = false) String accountId,
                                                               @RequestParam(value="locked", required = false) Boolean locked) {
        return this.statementsExt(accountId,locked);
    }

    @PostMapping(path="/ext/money/statement/lock")
    public @ResponseBody Iterable<StatementDTO> statementLockExt(@RequestBody StatementIdDTO statementId) throws InvalidStatementIdException, StatementAlreadyLockedException {
        return this.statementManager.statementLock(statementId,accountTransactionManager);
    }

    @PostMapping(path="/int/money/statement/lock")
    public @ResponseBody Iterable<StatementDTO> statementLockInt(@RequestBody StatementIdDTO statementId) throws InvalidStatementIdException, StatementAlreadyLockedException {
        return this.statementLockExt(statementId);
    }

    @PostMapping(path="/int/money/statement")
    public @ResponseBody Iterable<StatementDTO> createStatement(@RequestBody StatementDTO statement) throws StatementAlreadyExistsException, UpdateDeleteAccountException {
        LOG.info("Create a new statement - {}", statement);

        return this.statementManager.createStatement(statement);
    }

    @DeleteMapping(path="/int/money/statement")
    public @ResponseBody Iterable<StatementDTO> deleteStatement(@RequestBody StatementDTO statement) throws InvalidStatementIdException, CannotDeleteLockedStatementException, UpdateDeleteAccountException, CannotDeleteLastStatementException {
        LOG.info("Delete an account - {} {} {}", statement.getAccountId(), statement.getMonth(), statement.getYear());

        return this.statementManager.deleteStatement(statement,accountTransactionManager);
    }
}
