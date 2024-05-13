package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.NullOrBlankAccountIdException;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TransactionReportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportManager.class);

    private final AccountTransactionManager transactionManager;
    private final RegularPaymentManager regularPaymentManager;
    private final ReconciliationManager reconciliationManager;
    private final TransactionFilter filter;
    private final TransactionMapper mapper;
    private final ApplicationProperties applicationProperties;
    private final StatementRepository statementRepository;

    @Autowired
    public TransactionReportManager(AccountTransactionManager transactionManager,
                                    RegularPaymentManager regularPaymentManager,
                                    ReconciliationManager reconciliationManager,
                                    TransactionFilter filter,
                                    TransactionMapper mapper,
                                    StatementRepository statementRepository,
                                    ApplicationProperties applicationProperties) {
        this.transactionManager = transactionManager;
        this.regularPaymentManager = regularPaymentManager;
        this.reconciliationManager = reconciliationManager;
        this.filter = filter;
        this.mapper = mapper;
        this.statementRepository = statementRepository;
        this.applicationProperties = applicationProperties;
    }

    private String getDateString(LocalDate date) {
        return date.format(Constants.MONEY_DATE_FORMATTER);
    }

    private List<TransactionReportDTO> getPredicted(TransactionFilterDTO filter) {
        // If predicted are excluded then return empty list.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.FALSE)) {
            return new ArrayList<>();
        }

        ArrayList<TransactionReportDTO> result = new ArrayList<>();

        for(Regular next : regularPaymentManager.getAllRegularPayments()) {
            this.filter.passTransaction(next, filter).ifPresent(result::add);
        }

        return result;
    }

    private List<TransactionReportDTO> getFromReconciled(TransactionFilterDTO filter) throws NullOrBlankAccountIdException {
        // If reconciled are excluded then return empty list.
        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.FALSE)) {
            return new ArrayList<>();
        }

        // There must be a reconciliation account specified.
        if(filter.getReconciliationAccount() == null || filter.getReconciliationAccount().isEmpty()) {
            throw new NullOrBlankAccountIdException();
        }

        try {
            ArrayList<TransactionReportDTO> result = new ArrayList<>();

            for (MatchData next : reconciliationManager.match(filter.getReconciliationAccount())) {
                this.filter.passTransaction(next, filter).ifPresent(result::add);
            }

            return result;
        } catch (UpdateDeleteAccountException e) {
            return new ArrayList<>();
        }
    }

    private List<TransactionReportDTO> getStandardTransactions(TransactionFilterDTO filter) {
        // If predicated or from reconciled then we don't need standard transactions.
        if(filter.getPredicted() != null && filter.getPredicted().equals(Boolean.TRUE)) {
            return new ArrayList<>();
        }

        if(filter.getFromReconciled() != null && filter.getFromReconciled().equals(Boolean.TRUE)) {
            return new ArrayList<>();
        }

        ArrayList<TransactionReportDTO> result = new ArrayList<>();

        for(Transaction next : transactionManager.getAllTransactions()) {
            this.filter.passTransaction(next, filter).ifPresent(result::add);
        }

        return result;
    }

    private boolean includeStatement(Statement statement, List<AccountDTO> accounts, Boolean locked, StatementDateDTO statementDate) {
        if(accounts.isEmpty()) {
            return true;
        }

        // Check the account.
        boolean matchAccount = false;
        for(AccountDTO nextAccount : accounts) {
            if (statement.getId().getAccount().getId().equals(nextAccount.getId())) {
                matchAccount = true;
                break;
            }
        }

        if(!matchAccount) {
            return false;
        }

        // If specified, check the date.
        if(statementDate != null && (!statement.getId().getYear().equals(statementDate.getYear()) || !statement.getId().getMonth().equals(statementDate.getMonth()))) {
            return false;
        }

        // Check the locked status.
        return locked == null || locked.equals(statement.getLocked());
    }

    private FinancialAmount calculateOpeningBalance(TransactionFilterDTO filter) {
        // Opening balance not provided if the following filters are present.

        // If a value range is specified, then no opening balance.
        if(filter.getValueRange().getMinimum() != Double.NEGATIVE_INFINITY) {
            return new FinancialAmount();
        }
        if(filter.getValueRange().getMaximum() != Double.POSITIVE_INFINITY) {
            return new FinancialAmount();
        }

        // If a date range is specified, then no opening balance.
        if(filter.getDateRange().getFrom().equals(Constants.MONEY_EARLIEST_DATE_STRING)) {
            return new FinancialAmount();
        }
        if(filter.getDateRange().getTo().equals(Constants.MONEY_LATEST_DATE_STRING)) {
            return new FinancialAmount();
        }

        // If categories specified then no open balance.
        if(!filter.getCategories().isEmpty()) {
            return new FinancialAmount();
        }

        // Opening balance can be derived from the statements.
        double openingBalance = 0;

        List<Statement> includedStatements = new ArrayList<>();
        for(Statement statement : this.statementRepository.findAll()) {
            if(includeStatement(statement,filter.getAccounts(),filter.getLocked(),filter.getStatementDate())) {
                includedStatements.add(statement);
            }
        }

        // Get the oldest date.
        StatementDateDTO oldest = null;
        for(Statement statement : includedStatements) {
            if(oldest == null) {
                oldest = new StatementDateDTO();
                oldest.setYear(statement.getId().getYear());
                oldest.setMonth(statement.getId().getMonth());
            } else {
                // Is the next statement older?
                if(oldest.getYear() > statement.getId().getYear()) {
                    // Older year.
                    oldest.setYear(statement.getId().getYear());
                    oldest.setMonth(statement.getId().getMonth());
                } else if(oldest.getYear().equals(statement.getId().getYear()) && (oldest.getMonth() > statement.getId().getMonth())) {
                    // Same year, older month.
                    oldest.setMonth(statement.getId().getMonth());
                }
            }
        }

        // Some the opening balances from the oldest statements.
        for(Statement statement : includedStatements) {
            if(statement.getId().getYear().equals(oldest.getYear()) && statement.getId().getMonth().equals(oldest.getMonth())) {
                openingBalance += statement.getOpenBalance().getValue();
            }
        }

        // Opening balance only makes sense if the only filter relates to the statement.
        return new FinancialAmount(openingBalance);
    }

    private String calculateOpenDate(List<TransactionReportDTO> transactions) {
        if(!transactions.isEmpty()) {
            return transactions.get(0).getDate();
        }

        return "";
    }

    private FinancialAmount calculateTodayBalance(FinancialAmount openBalance, List<TransactionReportDTO> transactions) {
        LocalDate today = this.applicationProperties.getToday();

        double balance = openBalance.getValue();

        for(TransactionReportDTO transaction : transactions) {
            LocalDate transactionDate = mapper.map(transaction.getDate(),LocalDate.class);

            if(transactionDate.isBefore(today)) {
                balance += transaction.getAmount().getValue();
            }
        }

        return new FinancialAmount(balance);
    }

    private String calculateFutureDate(List<TransactionReportDTO> transactions) {
        // Get the last transaction.
        if(!transactions.isEmpty()) {
            TransactionReportDTO lastTransaction = transactions.get(transactions.size() - 1);

            LocalDate transactionDate = mapper.map(lastTransaction.getDate(), LocalDate.class);

            if (transactionDate.isAfter(applicationProperties.getToday())) {
                return mapper.map(lastTransaction.getDate(), String.class);
            }
        }

        return "";
    }

    private FinancialAmount calculateFutureBalance(FinancialAmount openBalance, List<TransactionReportDTO> transactions) {
        double balance = openBalance.getValue();

        for(TransactionReportDTO transaction : transactions) {
            balance += transaction.getAmount().getValue();
        }

        return new FinancialAmount(balance);
    }

    public TransactionDataDTO getTransactions(TransactionFilterDTO filter) throws NullOrBlankAccountIdException {
        LOG.info("Get Transactions based on {}",filter);

        TransactionDataDTO result = new TransactionDataDTO();

        // Add the transactions.
        result.getTransactions().addAll(getPredicted(filter));
        result.getTransactions().addAll(getFromReconciled(filter));
        result.getTransactions().addAll(getStandardTransactions(filter));

        // Remove any reconciled transaction where the transaction is also present, then sort the transactions.
        result.removeDuplicatesAndSort();

        // Calculate the opening details
        result.setOpenBalance(calculateOpeningBalance(filter));
        result.setOpenDate(calculateOpenDate(result.getTransactions()));

        // Calculate the balances on the transactions.
        double balance = result.getOpenBalance().getValue();
        for(TransactionReportDTO next : result.getTransactions()) {
            next.setBalance(new FinancialAmount(balance));

            balance += next.getAmount().getValue();
        }

        // Calculate today's date.
        result.setToday(getDateString(applicationProperties.getToday()));

        // Calculate today's balance.
        result.setTodayBalance(calculateTodayBalance(result.getOpenBalance(), result.getTransactions()));

        // Calculate future date.
        result.setForwardDate(calculateFutureDate(result.getTransactions()));

        // Calculate future balance.
        result.setForwardBalance(calculateFutureBalance(result.getOpenBalance(), result.getTransactions()));

        // Return the data.
        return result;
    }
}
