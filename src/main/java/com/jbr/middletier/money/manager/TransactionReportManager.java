package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.data.internal.repository.TransactionReportRepository;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.util.FinancialAmount;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TransactionReportManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionReportManager.class);

    private final AccountTransactionManager transactionManager;
    private final RegularPaymentManager regularPaymentManager;
    private final ReconciliationManager reconciliationManager;
    private final TransactionMapper mapper;
    private final ApplicationProperties applicationProperties;
    private final StatementRepository statementRepository;
    private final TransactionReportRepository transactionReportRepository;

    @Autowired
    public TransactionReportManager(AccountTransactionManager transactionManager,
                                    RegularPaymentManager regularPaymentManager,
                                    ReconciliationManager reconciliationManager,
                                    TransactionMapper mapper,
                                    StatementRepository statementRepository,
                                    TransactionReportRepository transactionReportRepository,
                                    ApplicationProperties applicationProperties) {
        this.transactionManager = transactionManager;
        this.regularPaymentManager = regularPaymentManager;
        this.reconciliationManager = reconciliationManager;
        this.mapper = mapper;
        this.statementRepository = statementRepository;
        this.applicationProperties = applicationProperties;
        this.transactionReportRepository = transactionReportRepository;
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

    private void getFromReconciled(String reconciliationAccount) {
        if(reconciliationAccount == null || reconciliationAccount.isEmpty()) {
            return;
        }

        // Get the transactions
        try {
            for (MatchData next : reconciliationManager.match(reconciliationAccount)) {
                if(next.getTransaction() == null) {
                    // Add data to the transaction report.
                    //TODO fix
                    TransactionReport tmp = this.mapper.map(next, TransactionReport.class);
                    this.transactionReportRepository.save(tmp);
                }
            }
        } catch (UpdateDeleteAccountException e) {
            LOG.error("Problem reading the reconciled data {}", e.getMessage());
        }
    }

    private void getStandardTransactions() {
        for(Transaction next : transactionManager.getAllTransactions()) {
            // Add this to the transaction report.
            // TODO fix
            TransactionReport tmp = mapper.map(next,TransactionReport.class);
            this.transactionReportRepository.save(tmp);
        }
    }

    private FinancialAmount calculateOpeningBalance(TransactionDataDTO transactionData) {
        // Determine the opening balance.
        return null;
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

    private FinancialAmount calculateTodayBalance(FinancialAmount openBalance, List<TransactionReportDTO> transactions) {
        if(openBalance == null) {
            return null;
        }

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
        if(openBalance == null) {
            return null;
        }

        double balance = openBalance.getValue();

        for(TransactionReportDTO transaction : transactions) {
            balance += transaction.getAmount().getValue();
        }

        return new FinancialAmount(balance);
    }

    private Specification<TransactionReport> findByCriteria(TransactionFilterDTO filter) {
        return new Specification<TransactionReport>() {
            @Override
            public Predicate toPredicate(Root<TransactionReport> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                if(filter.getPredicted() != null) {
                    predicates.add(cb.equal(root.get("predicted"),filter.getPredicted()));
                }

                if(filter.getFromReconciled() != null) {
                    predicates.add(cb.equal(root.get("fromReconciliation"),filter.getFromReconciled()));
                }

                if(filter.getLocked() != null) {
                    predicates.add(cb.equal(root.get("locked"),filter.getLocked()));
                }

                if(filter.getDescription() != null) {
                    predicates.add(cb.like(root.get("searchDescription"),"%"+filter.getDescription().toLowerCase().replaceAll("[^a-z0-9]","")+"%"));
                }

                if(filter.getStatementDate() != null) {
                    if(filter.getStatementDate().getMonth() != null) {
                        predicates.add(cb.equal(root.get("statementMonth"),filter.getStatementDate().getMonth()));
                    }
                    if(filter.getStatementDate().getYear() != null) {
                        predicates.add(cb.equal(root.get("statementYear"),filter.getStatementDate().getYear()));
                    }
                }

                if(filter.getCategories() != null && !filter.getCategories().isEmpty()) {
                    predicates.add(root.get("categoryId").in(filter.getCategories()));
                }

                if(filter.getDateRange() != null) {
                    if(filter.getDateRange().getFrom() != null && filter.getDateRange().getTo() != null) {
                        predicates.add(cb.between(root.get("date"),filter.getDateRange().getFrom(),filter.getDateRange().getTo()));
                    } else if(filter.getDateRange().getFrom() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("date"),filter.getDateRange().getFrom()));
                    } else if(filter.getDateRange().getTo() != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("date"),filter.getDateRange().getTo()));
                    }
                }

                if(filter.getValueRange() != null) {
                    if(filter.getValueRange().getMinimum() != null && filter.getValueRange().getMaximum() != null) {
                        predicates.add(cb.between(root.get("amount"),filter.getValueRange().getMinimum(),filter.getValueRange().getMaximum()));
                    } else if(filter.getValueRange().getMinimum() != null) {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("amount"),filter.getValueRange().getMinimum()));
                    } else if(filter.getValueRange().getMaximum() != null) {
                        predicates.add(cb.lessThanOrEqualTo(root.get("amount"),filter.getValueRange().getMaximum()));
                    }
                }

                if(filter.getAccounts() != null && !filter.getAccounts().isEmpty()) {
                    predicates.add(cb.equal(root.get("accountId"),filter.getAccounts()));
                }

                return cb.and(predicates.toArray(new Predicate[] {}));
            }
        };
    }

    public TransactionDataDTO getTransactions(TransactionFilterDTO filter) {
        LOG.info("Get Transactions based on {}",filter);

        /*
        TEST FILTER:
        {
            "fromReconciled":false,
            "predicted":false,
            "locked":false
        }
         */

        TransactionDataDTO result = new TransactionDataDTO();

        // Delete the transaction report details.
        this.transactionReportRepository.deleteAll();

        // Populate the transaction data.
//        getPredicted();
        getFromReconciled(filter.getReconciliationAccount());
        getStandardTransactions();

        //TODO
        // page

        for(TransactionReport x : this.transactionReportRepository.findAll()) {
            LOG.info(x.getDescription());
            if(x.getFromReconciliation()) {
                LOG.info(x.getDescription());
            }
        }

        // Get the transactions that meet the filter.
        int max = 50;
        for(TransactionReport next : this.transactionReportRepository.findAll(findByCriteria(filter))) {
            result.getTransactions().add(this.mapper.map(next,TransactionReportDTO.class));
            if(max-- == 0) {
                break;
            }
        }

        // Calculate the opening details
        result.setOpenBalance(calculateOpeningBalance(result));
        result.setOpenDate(calculateOpenDate(result.getTransactions()));

        if(result.getOpenBalance() != null) {
            // Calculate the balances on the transactions.
            double balance = result.getOpenBalance().getValue();
            for(TransactionReportDTO next : result.getTransactions()) {
                next.setBalance(new FinancialAmount(balance));

                balance += next.getAmount().getValue();
            }
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
