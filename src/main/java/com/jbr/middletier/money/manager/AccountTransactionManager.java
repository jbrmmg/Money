package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.DateRangeDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.events.CreateTransactionEvent;
import com.jbr.middletier.money.events.DeleteTransactionEvent;
import com.jbr.middletier.money.events.UpdateTransactionEvent;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.util.DateRange;
import com.jbr.middletier.money.util.FinancialAmount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.jbr.middletier.money.data.primary.repository.TransactionSpecifications.*;
import static com.jbr.middletier.money.data.primary.repository.TransactionSpecifications.categoryIn;


@Controller
public class AccountTransactionManager {
    private static final Logger LOG = LoggerFactory.getLogger(AccountTransactionManager.class);

    private final AccountManager accountManager;
    private final CategoryManager categoryManager;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public AccountTransactionManager(AccountManager accountManager,
                                     CategoryManager categoryManager,
                                     TransactionRepository transactionRepository,
                                     TransactionMapper transactionMapper, ApplicationEventPublisher applicationEventPublisher) {
        this.accountManager = accountManager;
        this.categoryManager = categoryManager;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public FinancialAmount getFinalBalanceForStatement(Statement statement) {
        List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                statement.getId().getAccount(),
                statement.getId().getYear(),
                statement.getId().getMonth());

        FinancialAmount balance = statement.getOpenBalance();

        for (Transaction nextTransaction : transactions ) {
            balance.increment(nextTransaction.getAmount());
            LOG.debug("Transaction (final balance) {}", nextTransaction.getAmount());
        }

        return balance;
    }

    public void removeTransactionsFromStatement(Statement statement) {
        List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                statement.getId().getAccount(),
                statement.getId().getYear(),
                statement.getId().getMonth());

        for(Transaction transaction : transactions) {
            transaction.setStatement(null);
        }

        transactionRepository.saveAll(transactions);
    }

    private Specification<Transaction> getReconciledTransactions(List<Account> accounts, LocalDate statementDate, List<Category> categories) throws InvalidTransactionSearchException {
        // Validate data.
        if((accounts == null)) {
            throw new InvalidTransactionSearchException("Must specify account");
        }

        if(statementDate == null){
            throw new InvalidTransactionSearchException("Must specify statement date");
        }

        // Reconciled transactions - for a particular month (statement), single account, list of categories.
        Specification<Transaction> search = Specification.where(statementDate(statementDate)).and(accountIn(accounts));

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getUnreconciledTransactions(List<Account> accounts, List<Category> categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(statementIsNull());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getAllTransactions(DateRange dateRange, List<Account> accounts, List<Category> categories) throws InvalidTransactionSearchException {
        // Validate data.
        if(dateRange.getFrom() == null || dateRange.getFrom().isEqual(Constants.MONEY_EARLIEST_DATE)){
            throw new InvalidTransactionSearchException("must specify a from date");
        }
        if(dateRange.getTo() == null || dateRange.getTo().isEqual(Constants.MONEY_LATEST_DATE)){
            throw new InvalidTransactionSearchException("must specify a to date");
        }

        // All transactions - between two dates, multiple accounts, list of categories
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(datesBetween(dateRange));

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getUnlockedTransactions(List<Account> accounts, List<Category> categories) {
        // Not locked transactions - no date, multiple accounts, list of categories
        Specification<Transaction> search = Specification.where(notLocked());

        if(accounts != null) {
            search = search.and(accountIn(accounts));
        }

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getTransactionSearch(TransactionRequestType type,
                                                            DateRange    dateRange,
                                                            List<String> categoryIds,
                                                            List<String> accountIds) throws InvalidTransactionSearchException {
        // Get the accounts
        List<Account> accounts = null;
        if(accountIds != null) {
            accounts = new ArrayList<>();
            for(Account next : accountManager.getAllExternal()) {
                if(accountIds.contains(next.getId())) {
                    accounts.add(next);
                }
            }
        }

        // Get the categories
        List<Category> categories = null;
        if(categoryIds != null) {
            categories = new ArrayList<>();
            for(Category next : categoryManager.getAllExternal()) {
                if(categoryIds.contains(next.getId())) {
                    categories.add(next);
                }
            }
        }

        // Process the request.
        switch (type) {
            case TRT_UNRECONCILED -> {
                LOG.info("Get Transaction - un reconciled");
                return getUnreconciledTransactions(accounts, categories);
            }
            case TRT_RECONCILED -> {
                LOG.info("Get Transaction - reconciled");
                return getReconciledTransactions(accounts, dateRange.getFrom(), categories);
            }
            case TRT_ALL -> {
                LOG.info("Get Transaction - all");
                return getAllTransactions(dateRange, accounts, categories);
            }
            case TRT_UNLOCKED -> {
                LOG.info("Get Transaction - unlocked");
                return getUnlockedTransactions(accounts, categories);
            }
            case TRT_UNKNOWN -> {
                LOG.info("Get Transaction - unknown");
                throw new IllegalStateException("Should never get here as all Enum values are catered for.");
            }
        }

        return null;
    }

    public List<TransactionDTO> getTransactions(TransactionRequestType type,
                                                DateRangeDTO externalDateRange,
                                                List<String> categoryIds,
                                                List<String> accountIds,
                                                boolean sortAscending) throws InvalidTransactionSearchException {
        if(type == TransactionRequestType.TRT_UNKNOWN) {
            // Just return an empty list.
            return new ArrayList<>();
        }

        Sort transactionSort = Sort.by(Sort.Direction.DESC,"date", "account", "amount");

        if(sortAscending) {
            transactionSort = Sort.by(Sort.Direction.ASC,"date", "account", "amount");
        }

        DateRange dateRange = transactionMapper.map(externalDateRange,DateRange.class);

        Specification<Transaction> specification = getTransactionSearch(type, dateRange, categoryIds, accountIds);

        List<TransactionDTO> result = new ArrayList<>();
        LOG.debug("Iterate over transactions");
        for(Transaction transaction : transactionRepository.findAll(Objects.requireNonNull(specification), transactionSort)) {
            LOG.debug("Transaction (getTransactions) {}", transaction.getId());
            result.add(transactionMapper.map(transaction,TransactionDTO.class));
        }

        return result;
    }

    private Transaction internalCreateTransaction(TransactionDTO transaction) throws InvalidTransactionException {
        Transaction newTransaction =  transactionMapper.map(transaction,Transaction.class);

        return saveTransaction(newTransaction);
    }

    public Transaction saveTransaction(Transaction transaction) throws InvalidTransactionException {
        // Check the account and category are valid.
        if(transaction.getAccount() == null) {
            throw new InvalidTransactionException("Missing account creating transaction.");
        }

        if(transaction.getCategory() == null) {
            throw new InvalidTransactionException("Missing category creating transaction.");
        }

        return transactionRepository.save(transaction);
    }

    private List<TransactionDTO> createIndividualTransaction(TransactionDTO transaction) throws InvalidTransactionException {
        List<TransactionDTO> result = new ArrayList<>();

        Transaction newTransaction = internalCreateTransaction(transaction);

        result.add(transactionMapper.map(newTransaction,TransactionDTO.class));

        // Fire event to create the new individual transaction.
        this.applicationEventPublisher.publishEvent(new CreateTransactionEvent(this, Collections.singletonList(newTransaction)));

        return result;
    }

    @Transactional
    public List<TransactionDTO> createTransaction(List<TransactionDTO> transaction) throws InvalidTransactionException {
        if(transaction.size() == 1) {
            return createIndividualTransaction(transaction.get(0));
        }

        // Must be a transfer - two transactions
        if(transaction.size() != 2) {
            throw new InvalidTransactionException("List size must be 1 or 2");
        }

        TransactionDTO from = transaction.get(0);
        TransactionDTO to = transaction.get(1);

        if(from.getAccountId().equals(to.getAccountId())) {
            throw new InvalidTransactionException("Transfer must be two different accounts");
        }

        // Category must be Transfer on both transactions
        Optional<Category> transfer = categoryManager.getIfValid(CategoryManager.CATEGORY_TRANSFER);
        if(transfer.isEmpty()) {
            throw new InvalidTransactionException("Cannot find the transfer category");
        }

        from.setCategoryId(CategoryManager.CATEGORY_TRANSFER);
        to.setCategoryId(CategoryManager.CATEGORY_TRANSFER);

        // Ensure the amount is the reverse
        to.setAmount(FinancialAmount.flipSign(from.getAmount()));
        to.setDate(from.getDate());

        // The transaction is either an individual transaction or it's a transfer
        Transaction fromTransaction = internalCreateTransaction(from);

        // Save the 'from' transaction and update the opposite id on the 'to'
        List<TransactionDTO> result = new ArrayList<>();
        result.add(transactionMapper.map(fromTransaction,TransactionDTO.class));
        to.setOppositeTransactionId(fromTransaction.getId());

        // Save the 'to' transaction and update the 'from' transaction.
        result.addAll(createIndividualTransaction(to));
        fromTransaction.setOppositeTransactionId(result.get(1).getId());
        result.get(0).setOppositeTransactionId(fromTransaction.getOppositeTransactionId());
        transactionRepository.save(fromTransaction);

        // Generate the event.
        this.applicationEventPublisher.publishEvent(new CreateTransactionEvent(this,Collections.singletonList(fromTransaction)));

        return result;
    }

    private boolean updateLocked(Boolean locked, Optional<Transaction> transaction) {
        // Only a transaction can affect the status
        if(transaction.isEmpty()) {
            return locked;
        }

        if(transaction.get().getStatement() == null) {
            return locked;
        }

        if(!transaction.get().getStatement().getLocked()) {
            return locked;
        }

        return true;
    }

    private void updateTransaction(Optional<Transaction> transaction, boolean locked, double factor, Optional<Category> category, TransactionDTO source) {
        if(transaction.isEmpty()) {
            return;
        }

        // Description can always be updated.
        transaction.get().setDescription(source.getDescription());

        // If there is a new category, update it.
        category.ifPresent(value -> transaction.get().setCategory(value));

        // If the transaction is locked, cannot change date or amount.
        if(locked) {
            return;
        }

        // Set the amount and the date.
        transaction.get().setDate(this.transactionMapper.map(source.getDate(),LocalDate.class));
        transaction.get().setAmount(source.getAmount().multiply(BigDecimal.valueOf(factor)));
    }

    public List<TransactionDTO> updateTransaction(TransactionDTO transaction) throws InvalidTransactionIdException, UpdateDeleteCategoryException {
        // Find the transaction.
        Optional<Transaction> existingTransaction = transactionRepository.findById(transaction.getId());
        Optional<Transaction> oppositeTransaction = Optional.empty();
        if(existingTransaction.isPresent()) {
            List<Transaction> toBeSaved = new ArrayList<>();

            toBeSaved.add(existingTransaction.get());

            // Is there an opposite transaction?
            if(existingTransaction.get().getOppositeTransactionId() != null) {
                oppositeTransaction = transactionRepository.findById(existingTransaction.get().getOppositeTransactionId());

                oppositeTransaction.ifPresent(toBeSaved::add);
            }

            // Get the transaction category (if this is not a transfer).
            Optional<Category> newCategory = Optional.empty();
            if(oppositeTransaction.isEmpty()) {
                newCategory = Optional.of(this.categoryManager.get(transaction.getCategoryId()));
            }

            // Is either transaction locked?
            boolean locked = updateLocked(false, existingTransaction);
            locked = updateLocked(locked, oppositeTransaction);

            // Update the transaction (date, amount, category and description).
            updateTransaction(existingTransaction, locked, 1.0, newCategory, transaction);
            updateTransaction(oppositeTransaction, locked, -1.0, newCategory, transaction);

            transactionRepository.saveAll(toBeSaved);
            this.applicationEventPublisher.publishEvent(new UpdateTransactionEvent(this,toBeSaved));

            List<TransactionDTO> result = new ArrayList<>();
            existingTransaction.ifPresent(value -> result.add(this.transactionMapper.map(value, TransactionDTO.class)));
            oppositeTransaction.ifPresent(value -> result.add(this.transactionMapper.map(value, TransactionDTO.class)));

            return result;
        }

        throw new InvalidTransactionIdException(transaction.getId());
    }

    public List<TransactionDTO> deleteTransaction(TransactionDTO transaction) throws InvalidTransactionIdException {
        LOG.info("Delete transaction.");

        // Get the transaction.
        Optional<Transaction> existingTransaction = transactionRepository.findById(transaction.getId());
        Optional<Transaction> oppositeTransaction = Optional.empty();
        boolean oppositeLocked = false;

        // Is there an opposite?
        if(existingTransaction.isPresent() && existingTransaction.get().getOppositeTransactionId() != null) {
            oppositeTransaction = transactionRepository.findById(existingTransaction.get().getOppositeTransactionId());

            if(oppositeTransaction.isPresent() && oppositeTransaction.get().reconciled()) {
                oppositeLocked = true;
            }
        }

        if(existingTransaction.isPresent() && !existingTransaction.get().reconciled() && !oppositeLocked) {
            // If the transaction is not reconciled then it can be deleted.
            List<Integer> deleteIds = new ArrayList<>();
            transactionRepository.deleteById(transaction.getId());
            deleteIds.add(transaction.getId());

            oppositeTransaction.ifPresent(value -> {
                transactionRepository.deleteById(value.getId());
                deleteIds.add(value.getId());
            });

            // Send delete event.
            this.applicationEventPublisher.publishEvent(new DeleteTransactionEvent(this,deleteIds));

            return new ArrayList<>();
        }

        throw new InvalidTransactionIdException(transaction.getId());
    }

    public List<Transaction> getInternalTransactionsForStatement(Account account, StatementId statementId) {
        return transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                account,
                statementId.getYear(),
                statementId.getMonth());
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>((Collection<? extends Transaction>) transactionRepository.findAll());
    }
}
