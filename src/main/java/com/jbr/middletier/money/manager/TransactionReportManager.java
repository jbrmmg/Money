package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.data.internal.repository.TransactionReportRepository;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.events.*;
import com.jbr.middletier.money.exceptions.NullOrBlankAccountIdException;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransactionSorting;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TransactionReportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportManager.class);

    private static final String DATE_COLUMN = "date";
    private static final String AMOUNT_COLUMN = "amount";
    private static final String STATEMENT_SORT_COLUMN = "statementSort";
    private static final String STATEMENT_YEAR_COLUMN = "statementYear";
    private static final String STATEMENT_MONTH_COLUMN = "statementMonth";
    private static final String ACCOUNT_ID_COLUMN = "accountId";
    private static final String CATEGORY_ID_COLUMN = "categoryId";
    private static final String PREDICTED_COLUMN = "predicted";
    private static final String FROM_RECONCILIATION_COLUMN = "fromReconciliation";
    private static final String LOCKED_COLUMN = "locked";
    private static final String SEARCH_DESCRIPTION_COLUMN = "searchDescription";

    private final AccountTransactionManager transactionManager;
    private final RegularPaymentManager regularPaymentManager;
    private final ReconciliationManager reconciliationManager;
    private final TransactionMapper mapper;
    private final ApplicationProperties applicationProperties;
    private final TransactionReportRepository transactionReportRepository;

    @Autowired
    public TransactionReportManager(AccountTransactionManager transactionManager,
                                    RegularPaymentManager regularPaymentManager,
                                    ReconciliationManager reconciliationManager,
                                    TransactionMapper mapper,
                                    TransactionReportRepository transactionReportRepository,
                                    ApplicationProperties applicationProperties) {
        this.transactionManager = transactionManager;
        this.regularPaymentManager = regularPaymentManager;
        this.reconciliationManager = reconciliationManager;
        this.mapper = mapper;
        this.applicationProperties = applicationProperties;
        this.transactionReportRepository = transactionReportRepository;
    }

    @PostConstruct
    public void initialise() {
        // Set up the in memory database.
        reset();
    }

    public void reset() {
        // Set up the in memory database.
        LOG.info("Initialising TransactionReportManager...");

        // Delete the transaction report details.
        this.transactionReportRepository.deleteAll();

        // Populate the transaction data (reconciliation must be last.).
        getPredicted();
        getStandardTransactions();
        getFromReconciled();
    }

    private String getDateString(LocalDate date) {
        return date.format(Constants.MONEY_DATE_FORMATTER);
    }

    private void getPredicted() {
        for(Regular next : regularPaymentManager.getAllRegularPayments()) {
            // Insert the regular payment into transaction report.
            this.transactionReportRepository.save(this.mapper.map(next, TransactionReport.class));
        }
    }

    private void getFromReconciled() {
        // Get the transactions
        try {
            for (MatchData next : reconciliationManager.match()) {
                if(next.getTransaction() == null) {
                    // Add data to the transaction report.
                    this.transactionReportRepository.save(this.mapper.map(next, TransactionReport.class));
                } else {
                    // Update the 'standard' transaction to be from reconciliation as well.
                    List<TransactionReport> standards = this.transactionReportRepository.findByTransactionId(next.getTransaction().getId());

                    for(TransactionReport standard : standards) {
                        standard.setFromReconciliation(true);
                        this.transactionReportRepository.save(standard);
                    }
                }
            }
        } catch (NullOrBlankAccountIdException e) {
            LOG.error("Cannot determine the reconciliation transactions. {}", e.getMessage());
        }
    }

    private void getStandardTransactions() {
        for(Transaction next : transactionManager.getAllTransactions()) {
            // Add this to the transaction report.
            this.transactionReportRepository.save(mapper.map(next,TransactionReport.class));
        }
    }

    private FinancialAmount calculateOpeningBalance(List<TransactionReportDTO> transactionData, TransactionFilterDTO filter) {
        // Determine the opening balance - this is only valid if there is no filter on the following:
        //   Value Range, Date Range, Category, Description, Predicted (true)
        if(filter.getValueRange() != null) {
            LOG.debug("No opening balance - value filter");
            return null;
        }

        if(filter.getDateRange() != null) {
            LOG.debug("No opening balance - date filter");
            return null;
        }

        if(filter.getCategories() != null && !filter.getCategories().isEmpty()) {
            LOG.debug("No opening balance - categories filter");
            return null;
        }

        if(filter.getDescription() != null && !filter.getDescription().isEmpty()) {
            LOG.debug("No opening balance - description filter");
            return null;
        }

        if(transactionData.isEmpty()) {
            LOG.debug("No opening balance - no transactions");
            return null;
        }

        if(filter.getPredicted() != null && filter.getPredicted()) {
            LOG.debug("No opening balance - predicted filter is true");
            return null;
        }

        // Opening balance is the sum of the earliest opening balance from each account present in the data.
        List<String> accountIds = new ArrayList<>();
        BigDecimal openBalance = BigDecimal.ZERO;
        for(TransactionReportDTO next : transactionData) {
            // Has this account been seen?
            if(!accountIds.contains(next.getAccount().getId()) && next.getStatement() != null && next.getStatement().getOpenBalance() != null) {
                openBalance = openBalance.add(next.getStatement().getOpenBalance().getValue());
                accountIds.add(next.getAccount().getId());
            }
        }

        return new FinancialAmount(openBalance);
    }

    private String calculateOpenDate(List<TransactionReportDTO> transactions) {
        // Opening date is the earliest date.
        if(!transactions.isEmpty()) {
            LocalDate earliestDate = LocalDate.parse(transactions.get(0).getDate(),Constants.MONEY_DATE_FORMATTER);

            for(TransactionReportDTO transaction : transactions) {
                LocalDate nextDate = LocalDate.parse(transaction.getDate(),Constants.MONEY_DATE_FORMATTER);

                if(nextDate.isBefore(earliestDate)) {
                    earliestDate = nextDate;
                }
            }

            return earliestDate.format(Constants.MONEY_DATE_FORMATTER);
        }

        return "";
    }

    private String calculateFutureDate(List<TransactionReportDTO> transactions) {
        // Get the last transaction.
        LocalDate futureDate = applicationProperties.getToday();
        if(!transactions.isEmpty()) {
            for(TransactionReportDTO next : transactions) {
                LocalDate transactionDate = mapper.map(next.getDate(), LocalDate.class);

                if(transactionDate.isAfter(futureDate)) {
                    futureDate = transactionDate;
                }
            }
        }

        if(futureDate.isAfter(applicationProperties.getToday())) {
            return mapper.map(futureDate, String.class);
        }

        return "";
    }

    private void addFlagPredicates(CriteriaBuilder cb, Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getPredicted() != null) {
            predicates.add(cb.equal(root.get(PREDICTED_COLUMN),filter.getPredicted()));
        }

        if(filter.getFromReconciled() != null) {
            predicates.add(cb.equal(root.get(FROM_RECONCILIATION_COLUMN),filter.getFromReconciled()));
        }

        if(filter.getLocked() != null) {
            predicates.add(cb.equal(root.get(LOCKED_COLUMN),filter.getLocked()));
        }
    }

    private void addDescriptionPredicate(CriteriaBuilder cb, Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getDescription() != null) {
            predicates.add(cb.like(root.get(SEARCH_DESCRIPTION_COLUMN),"%"+filter.getDescription().toLowerCase().replaceAll("[^a-z0-9]","")+"%"));
        }
    }

    private void addStatementPredicate(CriteriaBuilder cb, Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getStatementDate() != null) {
            if(filter.getStatementDate().getMonth() != null) {
                predicates.add(cb.equal(root.get(STATEMENT_MONTH_COLUMN),filter.getStatementDate().getMonth()));
            }
            if(filter.getStatementDate().getYear() != null) {
                predicates.add(cb.equal(root.get(STATEMENT_YEAR_COLUMN),filter.getStatementDate().getYear()));
            }
        }
    }

    private void addCategoryPredicate(Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getCategories() != null && !filter.getCategories().isEmpty()) {
            predicates.add(root.get(CATEGORY_ID_COLUMN).in(filter.getCategories().stream()
                    .map(CategoryDTO::getId)
                    .toList()));
        }
    }

    private void addDateRangePredicate(CriteriaBuilder cb, Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getDateRange() != null) {
            if(filter.getDateRange().getFrom() != null && filter.getDateRange().getTo() != null) {
                predicates.add(cb.between(root.get(DATE_COLUMN),filter.getDateRange().getFrom(),filter.getDateRange().getTo()));
            } else if(filter.getDateRange().getFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(DATE_COLUMN),filter.getDateRange().getFrom()));
            } else if(filter.getDateRange().getTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(DATE_COLUMN),filter.getDateRange().getTo()));
            }
        }
    }

    private void addValueRangePredicate(CriteriaBuilder cb, Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getValueRange() != null) {
            if(filter.getValueRange().getMinimum() != null && filter.getValueRange().getMaximum() != null) {
                predicates.add(cb.between(root.get(AMOUNT_COLUMN),filter.getValueRange().getMinimum(),filter.getValueRange().getMaximum()));
            } else if(filter.getValueRange().getMinimum() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(AMOUNT_COLUMN),filter.getValueRange().getMinimum()));
            } else if(filter.getValueRange().getMaximum() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(AMOUNT_COLUMN),filter.getValueRange().getMaximum()));
            }
        }
    }

    private void addAccountPredicate(Root<TransactionReport> root, TransactionFilterDTO filter, List<Predicate> predicates) {
        if(filter.getAccounts() != null && !filter.getAccounts().isEmpty()) {
            predicates.add(root.get(ACCOUNT_ID_COLUMN).in(filter.getAccounts().stream()
                    .map(AccountDTO::getId)
                    .toList()));
        }
    }

    private Specification<TransactionReport> findByCriteria(TransactionFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addFlagPredicates(cb,root,filter,predicates);
            addDescriptionPredicate(cb,root,filter,predicates);
            addStatementPredicate(cb,root,filter,predicates);
            addCategoryPredicate(root,filter,predicates);
            addDateRangePredicate(cb,root,filter,predicates);
            addValueRangePredicate(cb,root,filter,predicates);
            addAccountPredicate(root,filter,predicates);

            return cb.and(predicates.toArray(new Predicate[] {}));
        };
    }

    public List<TransactionReportDTO> getTransactions(TransactionFilterDTO filter) {
        LOG.info("Get Transactions based on {}",filter);

        List<TransactionReportDTO> result = new ArrayList<>();

        // Get the transactions that meet the filter.
        // JBR-444 - remove this when paging is implemented.
        int max = filter.getMaxPageSize();
        for(TransactionReport next : this.transactionReportRepository.findAll(findByCriteria(filter),Sort.by(Sort.Direction.ASC,STATEMENT_SORT_COLUMN,DATE_COLUMN,AMOUNT_COLUMN,ACCOUNT_ID_COLUMN))) {
            result.add(this.mapper.map(next,TransactionReportDTO.class));
            if(--max == 0) {
                break;
            }
        }

        // If possible add an opening balance row.
        FinancialAmount openingBalanceAmount = calculateOpeningBalance(result,filter);
        if(openingBalanceAmount != null) {
            TransactionReportDTO openingBalance = new TransactionReportDTO();
            openingBalance.setType(TransactionReportTypeDTO.OPEN_BALANCE);
            openingBalance.setBalance(openingBalanceAmount);
            openingBalance.setDate(calculateOpenDate(result));

            result.add(openingBalance);
        }

        // Add today's balance.
        TransactionReportDTO todayBalance = new TransactionReportDTO();
        todayBalance.setType(TransactionReportTypeDTO.TODAY_BALANCE);
        todayBalance.setDate(getDateString(applicationProperties.getToday()));
        result.add(todayBalance);

        // If there are transactions in the future, then add a future balance.
        String futureDate = calculateFutureDate(result);

        if(futureDate != null && !futureDate.isEmpty()) {
            TransactionReportDTO futureBalance = new TransactionReportDTO();
            futureBalance.setType(TransactionReportTypeDTO.FUTURE_BALANCE);
            futureBalance.setDate(futureDate);

            result.add(futureBalance);
        }

        // Sort the transactions as per the filter definition.
        result.sort((t1,t2) -> TransactionSorting.compare(t1,t2,filter.getTransactionSorts(),applicationProperties.getToday()));

        // Now we can calculate the balance information on each row.
        BigDecimal balance = ( openingBalanceAmount != null) ? openingBalanceAmount.getValue() : BigDecimal.ZERO;
        for(TransactionReportDTO next : result) {
            // If opening balance, then the balance is already known.
            if(next.getType() != TransactionReportTypeDTO.OPEN_BALANCE) {
                BigDecimal nextAmount = ( next.getAmount() != null ) ? next.getAmount().getValue() : BigDecimal.ZERO;
                next.setBalance(new FinancialAmount(balance.add(nextAmount)));

                balance = balance.add(nextAmount);
            }
        }

        // Return the data.
        return result;
    }

    private void updateReconData() {
        // Remove all transactions that are from reconciliation
        TransactionFilterDTO filter = new TransactionFilterDTO();
        filter.setFromReconciled(true);
        List<TransactionReport> updates = new ArrayList<>();
        List<TransactionReport> deletes = new ArrayList<>();
        for(TransactionReport next : this.transactionReportRepository.findAll(findByCriteria(filter))) {
            // If this is a real transaction that is also from reconciliation - then just remove the flag.
            if(next.getTransactionId() != null) {
                next.setFromReconciliation(false);
                updates.add(next);
            } else {
                deletes.add(next);
            }
        }

        // Save updates
        this.transactionReportRepository.saveAll(updates);

        // Delete the updates.
        this.transactionReportRepository.deleteAll(deletes);

        // Re-create the reconciliation transactions.
        getFromReconciled();
    }

    private void updateTransaction(Transaction next) {
        // Map this transaction to a report.
        TransactionReport updatedReport = this.mapper.map(next,TransactionReport.class);

        // Get the report transaction.
        List<TransactionReport> report = this.transactionReportRepository.findByTransactionId(next.getId());

        if(!report.isEmpty()) {
            for (TransactionReport nextReport : report) {
                // Save the updated details
                updatedReport.setId(nextReport.getId());

                this.transactionReportRepository.save(updatedReport);
            }
        } else {
            this.transactionReportRepository.save(updatedReport);
        }
    }

    @EventListener
    public void onUpdateTransaction(UpdateTransactionEvent update) {
        LOG.info("Update transaction in report table.");
        for(Transaction next : update.getTransactions()) {
            updateTransaction(next);
        }

        updateReconData();
    }

    @EventListener
    public void onDeleteTransaction(DeleteTransactionEvent delete) {
        LOG.info("Delete transaction in report table.");
        for(Integer next : delete.getTransactionIds()) {
            // Get the report transaction.
            List<TransactionReport> report = this.transactionReportRepository.findByTransactionId(next);

            // Delete details across and save.
            this.transactionReportRepository.deleteAll(report);
        }

        updateReconData();
    }

    @EventListener
    public void onReconcileTransactions(ReconcileTransactionEvent reconcile) {
        LOG.info("Update the transaction");
        for(Transaction next : reconcile.getTransactions()) {
            updateTransaction(next);
        }

        updateReconData();
    }

    @EventListener
    public void onReconciliationFileLoad(ReconciliationFileLoadEvent load) {
        LOG.info("Update the reconciled report.{}", load.getSource());

        updateReconData();
    }
}
