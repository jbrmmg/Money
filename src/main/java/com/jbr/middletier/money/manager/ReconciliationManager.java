package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.control.TransactionController;
import com.jbr.middletier.money.dataaccess.*;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;

@Controller
public class ReconciliationManager {
    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final TransactionController transactionController;
    private final ModelMapper modelMapper;

    public ReconciliationManager(ReconciliationRepository reconciliationRepository,
                                 CategoryRepository categoryRepository,
                                 AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 StatementRepository statementRepository,
                                 TransactionController transactionController,
                                 ModelMapper modelMapper) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.transactionController = transactionController;
        this.modelMapper = modelMapper;
    }
}
