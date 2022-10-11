package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.dto.TransactionWindowDTO;
import com.jbr.middletier.money.exceptions.*;
import com.jbr.middletier.money.util.FinancialAmount;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.*;
import static com.jbr.middletier.money.dataaccess.TransactionSpecifications.categoryIn;


@Controller
public class AccountTransactionManager {
    private static final Logger LOG = LoggerFactory.getLogger(AccountTransactionManager.class);

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public AccountTransactionManager(AccountRepository accountRepository,
                                     CategoryRepository categoryRepository,
                                     TransactionRepository transactionRepository,
                                     ModelMapper modelMapper) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.modelMapper = modelMapper;
    }

    private void getTransactionsInWindow(TransactionWindowDTO result, Account account, LocalDate end) {
        // Get the account balance at the start date.
        result.setStartBalance(getBalanceAt(account,result.getStart()));

        Pageable page = Pageable.ofSize(10);
        List<Transaction> transactions = this.transactionRepository.findByAccountAndDateBeforeOrderByDateDesc(account,result.getStart().plusDays(1), page);
        for(Transaction next : transactions) {
            if(next.getDate().equals(result.getStart())) {
                System.out.println(next.getId() + " " + next.getStatement().getId().getMonth() + "-" + next.getStatement().getId().getYear());
            }
        }
    }

    public TransactionWindowDTO getTransactionsInWindow(LocalDate start, LocalDate end) {
        TransactionWindowDTO result = new TransactionWindowDTO();
        result.setStart(start);

        // Perform on each account
        for(Account next : accountRepository.findAll()) {
            getTransactionsInWindow(result, next, end);
        }

        return result;
    }

    public FinancialAmount getBalanceAt(Account account, LocalDate asAtStartOf) {
        Map<String,Statement> possibleStatements = new HashMap<>();

        // Find the earliest statement with the transactions we are interested in.
        boolean keepLooking = true;
        Pageable page = Pageable.ofSize(10);
        while(keepLooking) {
            for (Transaction next : transactionRepository.findByAccountAndDateBeforeOrderByDateDesc(account, asAtStartOf.plusDays(1), page)) {
                if (next.getDate().equals(asAtStartOf)) {
                    if ((next.getStatement() != null) && (!possibleStatements.containsKey(next.getStatement().getId().toString()))) {
                        possibleStatements.put(next.getStatement().getId().toString(),next.getStatement());
                    }
                } else {
                    keepLooking = false;
                }
            }
        }

        // If no statements were found.

        return new FinancialAmount(0);
    }

    public FinancialAmount getFinalBalanceForStatement(Statement statement) {
        List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                statement.getId().getAccount(),
                statement.getId().getYear(),
                statement.getId().getMonth());

        FinancialAmount balance = statement.getOpenBalance();

        for (Transaction nextTransaction : transactions ) {
            balance.increment(nextTransaction.getAmount());
            LOG.debug("Transaction {}", nextTransaction.getAmount());
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

    private Specification<Transaction> getReconciledTransactions(Iterable<Account> accounts, LocalDate statmentDate, Iterable<Category> categories) throws InvalidTransactionSearchException {
        // Validate data.
        if((accounts == null)) {
            throw new InvalidTransactionSearchException("Must specify account");
        }

        if(statmentDate == null){
            throw new InvalidTransactionSearchException("Must specify statement date");
        }

        // Reconciled transactions - for a particular month (statement), single account, list of categories.
        Specification<Transaction> search = Specification.where(statementDate(statmentDate)).and(accountIn(accounts));

        if(categories != null) {
            search = search.and(categoryIn(categories));
        }

        return search;
    }

    private Specification<Transaction> getUnreconciledTransactions(Iterable<Account> accounts, Iterable<Category> categories) {
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

    private Specification<Transaction> getAllTransactions(DateRange dateRange, Iterable<Account> accounts, Iterable<Category> categories) throws InvalidTransactionSearchException {
        // Validate data.
        if(dateRange.getFrom() == null){
            throw new InvalidTransactionSearchException("must specify a from date");
        }
        if(dateRange.getTo() == null){
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

    private Specification<Transaction> getUnlockedTransactions(Iterable<Account> accounts, Iterable<Category> categories) {
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
                                                            DateRange dateRange,
                                                            List<String> categoryIds,
                                                            List<String> accountIds) throws InvalidTransactionSearchException {
        // Get the accounts
        Iterable<Account> accounts = null;
        if(accountIds != null) {
            accounts = accountRepository.findAllById(accountIds);
        }

        // Get the categories
        Iterable<Category> categories = null;
        if(categoryIds != null) {
            categories = categoryRepository.findAllById(categoryIds);
        }

        // Process the request.
        switch (type) {
            case TRT_UNRECONCILED:
                LOG.info("Get Transaction - un reconciled");
                return getUnreconciledTransactions(accounts, categories);
            case TRT_RECONCILED:
                LOG.info("Get Transaction - reconciled");
                return getReconciledTransactions(accounts, dateRange.getFrom(), categories);
            case TRT_ALL:
                LOG.info("Get Transaction - all");
                return getAllTransactions(dateRange, accounts, categories);
        }

        LOG.info("Get Transaction - unlocked");
        return getUnlockedTransactions(accounts, categories);
    }

    public List<TransactionDTO> getTransactions(TransactionRequestType type,
                                                DateRange dateRange,
                                                List<String> categoryIds,
                                                List<String> accountIds,
                                                boolean sortAscending) throws InvalidTransactionSearchException {
        Sort transactionSort = Sort.by(Sort.Direction.DESC,"date", "account", "amount");

        if(sortAscending) {
            transactionSort = Sort.by(Sort.Direction.ASC,"date", "account", "amount");
        }

        Specification<Transaction> specification = getTransactionSearch(type, dateRange, categoryIds, accountIds);

        List<TransactionDTO> result = new ArrayList<>();
        for(Transaction transaction : transactionRepository.findAll(specification, transactionSort)) {
            result.add(modelMapper.map(transaction,TransactionDTO.class));
        }

        return result;
    }

    private List<TransactionDTO> createIndividualTransaction(TransactionDTO transaction) throws InvalidTransactionException, InvalidAccountIdException, InvalidCategoryIdException {
        List<TransactionDTO> result = new ArrayList<>();

        // Check the account and category are valid.
        Optional<Account> account = accountRepository.findById(transaction.getAccount().getId());
        if(!account.isPresent()) {
            throw new InvalidAccountIdException(transaction.getAccount());
        }

        Optional<Category> category = categoryRepository.findById(transaction.getCategory().getId());
        if(!category.isPresent()) {
            throw new InvalidCategoryIdException(transaction.getCategory());
        }

        Transaction newTransaction = transactionRepository.save(modelMapper.map(transaction,Transaction.class));

        result.add(modelMapper.map(newTransaction,TransactionDTO.class));
        return result;
    }

    @Transactional
    protected List<TransactionDTO> createTransferTransaction(TransactionDTO from, TransactionDTO to) throws InvalidTransactionException, InvalidCategoryIdException, InvalidAccountIdException {
        // Save the 'from' transaction and update the opposite id on the 'to'
        List<TransactionDTO> result = new ArrayList<>(createIndividualTransaction(from));
        to.setOppositeTransactionId(result.get(0).getId());

        // Save the 'to' transaction and update the 'from' transaction.
        result.addAll(createIndividualTransaction(to));
        result.get(0).setOppositeTransactionId(result.get(1).getOppositeTransactionId());
        transactionRepository.save(modelMapper.map(result.get(0),Transaction.class));

        return result;
    }

    public List<TransactionDTO> createTransaction(List<TransactionDTO> transaction) throws InvalidTransactionException, InvalidCategoryIdException, InvalidAccountIdException {
        if(transaction.size() == 1) {
            return createIndividualTransaction(transaction.get(0));
        }

        // Must be a transfer - two transactions
        if(transaction.size() != 2) {
            throw new InvalidTransactionException("List size must be 1 or 2");
        }

        TransactionDTO from = transaction.get(0);
        TransactionDTO to = transaction.get(1);

        if(from.getAccount().getId().equals(to.getAccount().getId())) {
            throw new InvalidTransactionException("Transfer must be two different accounts");
        }

        // Category must be Transfer on both transactions
        Optional<Category> transfer = categoryRepository.findById("TRF");
        if(!transfer.isPresent()) {
            throw new InvalidTransactionException("Cannot find the transfer category");
        }

        from.setCategory(modelMapper.map(transfer.get(), CategoryDTO.class));
        to.setCategory(modelMapper.map(transfer.get(), CategoryDTO.class));

        // Ensure the amount is the reverse
        to.setAmount(from.getAmount() * -1);

        // The transaction is either an individual transaction or it's a transfer
        return createTransferTransaction(from,to);
    }

    public List<TransactionDTO> updateTransaction(TransactionDTO transaction) throws InvalidTransactionIdException, InvalidCategoryIdException {
        // Find the transaction.
        Optional<Transaction> existingTransaction = transactionRepository.findById(transaction.getId());
        if(existingTransaction.isPresent()) {
            List<TransactionDTO> result = new ArrayList<>();
            List<Transaction> toBeSaved = new ArrayList<>();

            result.add(modelMapper.map(existingTransaction.get(),TransactionDTO.class));
            toBeSaved.add(existingTransaction.get());

            // Is there an opposite transaction?
            if(existingTransaction.get().getOppositeTransactionId() != null) {
                Optional<Transaction> oppositeTransaction = transactionRepository.findById(existingTransaction.get().getOppositeTransactionId());

                if(oppositeTransaction.isPresent()) {
                    result.add(modelMapper.map(oppositeTransaction.get(), TransactionDTO.class));
                    toBeSaved.add(oppositeTransaction.get());
                }
            }

            // Perform the update
            for(Transaction next : toBeSaved) {
                next.setAmount(transaction.getAmount());
                next.setDescription(transaction.getDescription());

                if(toBeSaved.size() == 1) {
                    Optional<Category> category = categoryRepository.findById(transaction.getCategory().getId());

                    if(!category.isPresent()) {
                        throw new InvalidCategoryIdException(transaction.getCategory().getId());
                    }

                    next.setCategory(category.get());
                }
            }

            transactionRepository.saveAll(toBeSaved);

            return result;
        }

        throw new InvalidTransactionIdException(transaction.getId());
    }

    public List<TransactionDTO> deleteTransaction(TransactionDTO transaction) throws InvalidTransactionIdException {
        LOG.info("Delete transaction.");

        // Get the transaction.
        Optional<Transaction> existingTransaction = transactionRepository.findById(transaction.getId());

        if(existingTransaction.isPresent() && !existingTransaction.get().reconciled()) {
            // If the transaction is not reconciled then it can be deleted.
            transactionRepository.deleteById(transaction.getId());
            return new ArrayList<>();
        }

        throw new InvalidTransactionIdException(transaction.getId());
    }
}
