package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.StatementDTO;
import com.jbr.middletier.money.dto.StatementIdDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.StatementManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Statements", description = "Account statement management and locking")
@Validated
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

    @GetMapping(path="/statement")
    public Iterable<StatementDTO> getStatements(@RequestParam(value="accountId", required = false) @Pattern(regexp="^[a-zA-Z]{4}$",message="Id must be a four letter code") String accountId,
                                                @RequestParam(value="locked", required = false) Boolean locked) {
        return this.statementManager.getStatements(accountId,locked);
    }

    @Operation(summary = "Lock a statement to prevent further modifications")
    @PostMapping(path="/statement/lock")
    public Iterable<StatementDTO> statementLock(@Valid @RequestBody StatementIdDTO statementId) throws InvalidStatementIdException, StatementAlreadyLockedException {
        return this.statementManager.statementLock(statementId,accountTransactionManager);
    }

    @PostMapping(path="/statement")
    public Iterable<StatementDTO> createStatement(@Valid @RequestBody StatementDTO statement) throws StatementAlreadyExistsException, UpdateDeleteAccountException {
        LOG.info("Create a new statement - {}", statement);
        return this.statementManager.createStatement(statement);
    }

    @DeleteMapping(path="/statement")
    public Iterable<StatementDTO> deleteStatement(@Valid @RequestBody StatementDTO statement) throws InvalidStatementIdException, CannotDeleteLockedStatementException, UpdateDeleteAccountException, CannotDeleteLastStatementException {
        LOG.info("Delete a statement - {}", statement);
        return this.statementManager.deleteStatement(statement,accountTransactionManager);
    }
}
