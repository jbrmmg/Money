package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.*;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.schedule.AdjustmentType;
import com.jbr.middletier.money.util.DateRange;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.utils.UtilityMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;

@SpringBootTest(classes = MiddleTier.class)
class PoJoTest {
    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private StatementMapper statementMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private RegularMapper regularMapper;

    @Autowired
    private UtilityMapper utilityMapper;

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private CategoryManager categoryManager;

    @Autowired
    private StatementRepository statementRepository;

    @Test
    void accountToDTO() {
        Account account = new Account();
        account.setId("CHEESE");
        account.setColour("BLACK");
        account.setImagePrefix("Cheese");
        account.setName("Testing");
        account.setClosed(true);
        account.setTransferAccountId("BANK");
        account.setTransferDay(2);
        account.setWeekendAdj(AdjustmentType.AT_FORWARD);
        AccountDTO accountDTO = accountMapper.map(account, AccountDTO.class);
        Assertions.assertEquals("CHEESE", accountDTO.getId());
        Assertions.assertEquals("BLACK",accountDTO.getColour());
        Assertions.assertEquals("Testing",accountDTO.getName());
        Assertions.assertEquals("Cheese",accountDTO.getImagePrefix());
        Assertions.assertTrue(accountDTO.getClosed());

        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        boolean test = accountDTO.equals("Test");
        Assertions.assertFalse(test);
    }

    @Test
    void accountFromDTO() {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("HOPE");
        accountDTO.setColour("BLUE");
        accountDTO.setImagePrefix("Cheese");
        accountDTO.setName("Testing");
        accountDTO.setClosed(false);
        Account account = accountMapper.map(accountDTO,Account.class);
        Assertions.assertEquals("HOPE", account.getId());
        Assertions.assertEquals("BLUE",account.getColour());
        Assertions.assertEquals("Testing",account.getName());
        Assertions.assertEquals("Cheese",account.getImagePrefix());
        Assertions.assertFalse(account.getClosed());
        Assertions.assertNull(account.getTransferAccountId());
        Assertions.assertNull(account.getWeekendAdj());
        Assertions.assertNull(account.getTransferDay());
    }

    @Test
    void categoryToDTO() {
        Category category = new Category();
        category.setId("HOTEL");
        category.setColour("WHITE");
        category.setName("Test");
        category.setExpense(true);
        category.setGroup("GRP");
        category.setRestricted(true);
        category.setSort(100L);
        category.setSystemUse(true);
        CategoryDTO categoryDTO = categoryMapper.map(category, CategoryDTO.class);
        Assertions.assertEquals("HOTEL",categoryDTO.getId());
        Assertions.assertEquals("WHITE",categoryDTO.getColour());
        Assertions.assertEquals("Test",categoryDTO.getName());
        Assertions.assertTrue(categoryDTO.getExpense());
        Assertions.assertEquals("GRP",categoryDTO.getGroup());
        Assertions.assertTrue(categoryDTO.getRestricted());
        Assertions.assertEquals(100L,categoryDTO.getSort().longValue());
        Assertions.assertTrue(categoryDTO.getSystemUse());
    }

    @Test
    void categoryFromDTO() {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId("AROSE");
        categoryDTO.setColour("PINK");
        categoryDTO.setName("Test");
        categoryDTO.setExpense(true);
        categoryDTO.setGroup("GRP");
        categoryDTO.setRestricted(true);
        categoryDTO.setSort(100L);
        categoryDTO.setSystemUse(true);
        Category category = categoryMapper.map(categoryDTO, Category.class);
        Assertions.assertEquals("AROSE",category.getId());
        Assertions.assertEquals("PINK",category.getColour());
        Assertions.assertEquals("Test",category.getName());
        Assertions.assertTrue(category.getExpense());
        Assertions.assertEquals("GRP",category.getGroup());
        Assertions.assertTrue(category.getRestricted());
        Assertions.assertEquals(100L,category.getSort().longValue());
        Assertions.assertTrue(category.getSystemUse());
    }

    @Test
    void statementIdToDTO() {
        Account account = new Account();
        account.setId("FLIP");

        StatementId statementId = new StatementId();
        statementId.setAccount(account);
        statementId.setMonth(10);
        statementId.setYear(2003);

        StatementIdDTO statementIdDTO = statementMapper.map(statementId,StatementIdDTO.class);
        Assertions.assertEquals("FLIP",statementIdDTO.getAccountId());
        Assertions.assertEquals(10,statementIdDTO.getMonth().intValue());
        Assertions.assertEquals(2003,statementIdDTO.getYear().intValue());
    }

    public void statementIdFromDTO() {
        StatementIdDTO statementIdDTO = new StatementIdDTO("BANK", 7, 2019);
        StatementId statementId = statementMapper.map(statementIdDTO,StatementId.class);
        Assertions.assertEquals("BANK",statementId.getAccount().getId());
        Assertions.assertEquals(7,statementId.getMonth().intValue());
        Assertions.assertEquals(2019,statementId.getYear().intValue());
    }

    @Test
    void compareStatementIdDTO() {
        StatementIdDTO lhs = new StatementIdDTO("BANK",5, 2011);
        Assertions.assertEquals(0,lhs.compareTo(new StatementIdDTO("bank",5,2011)));
        Assertions.assertEquals(-31,lhs.compareTo(new StatementIdDTO("a", 5, 2011)));
        Assertions.assertEquals(1,lhs.compareTo(new StatementIdDTO("bank", 4, 2011)));
        Assertions.assertEquals(1,lhs.compareTo(new StatementIdDTO("bank", 5, 2010)));
        Assertions.assertEquals(-33,lhs.compareTo(new StatementIdDTO("clown", 5, 2011)));
        Assertions.assertEquals(-1,lhs.compareTo(new StatementIdDTO("bank", 6, 2011)));
        Assertions.assertEquals(-1,lhs.compareTo(new StatementIdDTO("bank", 5, 2012)));

        Assertions.assertEquals(lhs, new StatementIdDTO("bank", 5, 2011));

        Assertions.assertEquals(lhs.hashCode(),new StatementIdDTO("bank",5,2011).hashCode());

        Assertions.assertEquals("BANK.201105", lhs.toString());
    }

    @Test
    void compareStatementId() {
        Account account1 = new Account();
        account1.setId("BANK");

        Account account2 = new Account();
        account2.setId("a");

        Account account3 = new Account();
        account3.setId("clown");

        StatementId lhs = new StatementId(account1,2011, 5);
        Assertions.assertEquals(lhs, new StatementId(account1,2011,5));
        Assertions.assertNotEquals(lhs, new StatementId(account2, 2011, 5));
        Assertions.assertNotEquals(lhs, new StatementId(account1, 2011, 4));
        Assertions.assertNotEquals(lhs, new StatementId(account1, 2010, 5));
        Assertions.assertNotEquals(lhs, new StatementId(account3, 2011, 5));
        Assertions.assertNotEquals(lhs, new StatementId(account1, 2011, 6));
        Assertions.assertNotEquals(lhs, new StatementId(account1, 2012, 5));

        Account account1a = new Account();
        account1a.setId("bank");

        Assertions.assertEquals(lhs.hashCode(),new StatementId(account1a,2011,5).hashCode());

        Assertions.assertEquals("BANK201105", lhs.toString());
    }

    @Test
    void statementToDTO() {
        Account account = new Account();
        account.setId("BARCLAY");
        Statement statement = new Statement(account,1,2022, BigDecimal.valueOf(101.23),true);
        StatementDTO statementDTO = statementMapper.map(statement,StatementDTO.class);
        Assertions.assertEquals("BARCLAY",statementDTO.getAccountId());
        Assertions.assertEquals(1,statementDTO.getMonth().intValue());
        Assertions.assertEquals(2022,statementDTO.getYear().intValue());
        Assertions.assertTrue(statementDTO.getLocked());
        Assertions.assertEquals(101.23,statementDTO.getOpenBalance().getValue().doubleValue(),0.001);
    }

    @Test
    void statementFromDTO() {
        StatementDTO statementDTO = new StatementDTO();
        statementDTO.setAccountId("BANK");
        statementDTO.setMonth(2);
        statementDTO.setYear(2021);
        statementDTO.setLocked(true);
        statementDTO.setOpenBalance(new FinancialAmount(BigDecimal.valueOf(102.12)));
        Statement statement = statementMapper.map(statementDTO,Statement.class);
        Assertions.assertEquals("BANK",statement.getId().getAccount().getId());
        Assertions.assertEquals(2,statement.getId().getMonth().intValue());
        Assertions.assertEquals(2021,statement.getId().getYear().intValue());
        Assertions.assertTrue(statement.getLocked());
        Assertions.assertEquals(102.12,statement.getOpenBalance().getValue().doubleValue(),0.001);
    }

    @Test
    void transactionToDTO() {
        Account account = new Account();
        account.setId("FLIP");
        Category category = new Category();
        category.setId("FLOP");
        Statement statement = new Statement(account,1,2022,BigDecimal.valueOf(101.23),true);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(statement);
        transaction.setOppositeTransactionId(92);
        transaction.setAmount(BigDecimal.valueOf(1.29));
        transaction.setDescription("Testing");
        transaction.setDate(LocalDate.of(2018, Month.OCTOBER,7));
        TransactionDTO transactionDTO = transactionMapper.map(transaction, TransactionDTO.class);
        Assertions.assertEquals("FLIP",transactionDTO.getAccountId());
        Assertions.assertEquals("FLOP",transactionDTO.getCategoryId());
        Assertions.assertEquals(2022,transactionDTO.getStatementYear().intValue());
        Assertions.assertEquals(1,transactionDTO.getStatementMonth().intValue());
        Assertions.assertEquals(92,transactionDTO.getOppositeTransactionId().intValue());
        Assertions.assertEquals(1.29,transactionDTO.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("Testing",transactionDTO.getDescription());
        Assertions.assertEquals("2018-10-07",transactionDTO.getDate());
        Assertions.assertTrue(transactionDTO.getHasStatement());
        Assertions.assertTrue(transactionDTO.getStatementLocked());
    }

    @Test
    void transactionToDTO2() {
        Account account = new Account();
        account.setId("FLIP");
        Category category = new Category();
        category.setId("FLOP");
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(null);
        transaction.setOppositeTransactionId(92);
        transaction.setAmount(BigDecimal.valueOf(1.29));
        transaction.setDescription("Testing");
        transaction.setDate(LocalDate.of(2018, Month.OCTOBER,7));
        TransactionDTO transactionDTO = transactionMapper.map(transaction, TransactionDTO.class);
        Assertions.assertEquals("FLIP",transactionDTO.getAccountId());
        Assertions.assertEquals("FLOP",transactionDTO.getCategoryId());
        Assertions.assertNull(transactionDTO.getStatementYear());
        Assertions.assertNull(transactionDTO.getStatementMonth());
        Assertions.assertEquals(92,transactionDTO.getOppositeTransactionId().intValue());
        Assertions.assertEquals(1.29,transactionDTO.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("Testing",transactionDTO.getDescription());
        Assertions.assertEquals("2018-10-07",transactionDTO.getDate());
        Assertions.assertFalse(transactionDTO.getHasStatement());
        Assertions.assertFalse(transactionDTO.getStatementLocked());
    }

    @Test
    void transactionFromDTO() throws UpdateDeleteAccountException {
        Account account = accountManager.get("BANK");

        Statement testStatement = new Statement();
        StatementId testStatementId = new StatementId();
        testStatementId.setAccount(account);
        testStatementId.setMonth(8);
        testStatementId.setYear(2021);
        testStatement.setId(testStatementId);
        testStatement.setOpenBalance(BigDecimal.ZERO);
        testStatement.setLocked(false);

        statementRepository.save(testStatement);

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAccountId("BANK");
        transactionDTO.setCategoryId("HSE");
        transactionDTO.setStatementMonth(8);
        transactionDTO.setStatementYear(2021);
        transactionDTO.setOppositeTransactionId(92);
        transactionDTO.setAmount(BigDecimal.valueOf(1.29));
        transactionDTO.setDescription("Testing");
        transactionDTO.setDate("2018-07-23");
        Transaction transaction = transactionMapper.map(transactionDTO, Transaction.class);
        Assertions.assertEquals("BANK",transaction.getAccount().getId());
        Assertions.assertEquals("HSE",transaction.getCategory().getId());
        Assertions.assertEquals("BANK",transaction.getStatement().getId().getAccount().getId());
        Assertions.assertEquals(2021,transaction.getStatement().getId().getYear().intValue());
        Assertions.assertEquals(8,transaction.getStatement().getId().getMonth().intValue());
        Assertions.assertEquals(92,transaction.getOppositeTransactionId().intValue());
        Assertions.assertEquals(1.29,transaction.getAmount().getValue().doubleValue(),0.001);
        Assertions.assertEquals("Testing",transaction.getDescription());
        Assertions.assertEquals(LocalDate.of(2018, Month.JULY,23),transaction.getDate());

        statementRepository.delete(testStatement);
    }

    @Test
    void RegularToDTO() {
        Account account = new Account();
        account.setId("123");
        Category category = new Category();
        category.setId("456");
        Regular regular = new Regular();
        regular.setAccount(account);
        regular.setCategory(category);
        regular.setAmount(BigDecimal.valueOf(10.20));
        regular.setFrequency("1W");
        regular.setDescription("Testing");
        regular.setStart(LocalDate.of(2019, Month.FEBRUARY,5));
        regular.setLastDate(LocalDate.of(2019, Month.MARCH,5));
        regular.setWeekendAdj(AdjustmentType.AT_BACKWARD);
        RegularDTO regularDTO = regularMapper.map(regular,RegularDTO.class);
        Assertions.assertEquals("123",regularDTO.getAccountId());
        Assertions.assertEquals("456",regularDTO.getCategoryId());
        Assertions.assertEquals(10.20,regularDTO.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("1W",regularDTO.getFrequency());
        Assertions.assertEquals("Testing",regularDTO.getDescription());
        Assertions.assertEquals(AdjustmentType.AT_BACKWARD.toString(),regularDTO.getWeekendAdj());
        Assertions.assertEquals("2019-02-05",regularDTO.getStart());
        Assertions.assertEquals("2019-03-05",regularDTO.getLastDate());
    }

    @Test
    void RegularToDTO2() {
        Account account = new Account();
        account.setId("123");
        Category category = new Category();
        category.setId("456");
        Regular regular = new Regular();
        regular.setAccount(account);
        regular.setCategory(category);
        regular.setAmount(BigDecimal.valueOf(10.20));
        regular.setFrequency("1W");
        regular.setDescription("Testing");
        regular.setStart(LocalDate.of(2019, Month.FEBRUARY,5));
        regular.setWeekendAdj(AdjustmentType.AT_BACKWARD);
        RegularDTO regularDTO = regularMapper.map(regular,RegularDTO.class);
        Assertions.assertEquals("123",regularDTO.getAccountId());
        Assertions.assertEquals("456",regularDTO.getCategoryId());
        Assertions.assertEquals(10.20,regularDTO.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("1W",regularDTO.getFrequency());
        Assertions.assertEquals("Testing",regularDTO.getDescription());
        Assertions.assertEquals(AdjustmentType.AT_BACKWARD.toString(),regularDTO.getWeekendAdj());
        Assertions.assertEquals("2019-02-05",regularDTO.getStart());
        Assertions.assertNull(regularDTO.getLastDate());
    }

    @Test
    void RegularFromDTO()  {
        RegularDTO regularDTO = new RegularDTO();
        regularDTO.setAccountId("BANK");
        regularDTO.setCategoryId("FDG");
        regularDTO.setAmount(BigDecimal.valueOf(10.20));
        regularDTO.setFrequency("1W");
        regularDTO.setDescription("Testing");
        regularDTO.setStart("2019-04-03");
        regularDTO.setLastDate("2019-05-10");
        regularDTO.setWeekendAdj("FW");
        Regular regular = regularMapper.map(regularDTO,Regular.class);
        Assertions.assertEquals("BANK",regular.getAccount().getId());
        Assertions.assertEquals("FDG",regular.getCategory().getId());
        Assertions.assertEquals(10.20,regular.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("1W",regular.getFrequency());
        Assertions.assertEquals("Testing",regular.getDescription());
        Assertions.assertEquals(AdjustmentType.AT_FORWARD,regular.getWeekendAdj());
        Assertions.assertEquals(LocalDate.of(2019, Month.APRIL,3),regular.getStart());
        Assertions.assertEquals(LocalDate.of(2019, Month.MAY,10),regular.getLastDate());
    }

    @Test
    void RegularFromDTO2()  {
        RegularDTO regularDTO = new RegularDTO();
        regularDTO.setAccountId("BANK");
        regularDTO.setCategoryId("FDG");
        regularDTO.setAmount(BigDecimal.valueOf(10.20));
        regularDTO.setFrequency("1W");
        regularDTO.setDescription("Testing");
        regularDTO.setStart("2019-04-03");
        regularDTO.setWeekendAdj("FW");
        Regular regular = regularMapper.map(regularDTO,Regular.class);
        Assertions.assertEquals("BANK",regular.getAccount().getId());
        Assertions.assertEquals("FDG",regular.getCategory().getId());
        Assertions.assertEquals(10.20,regular.getAmount().doubleValue(),0.001);
        Assertions.assertEquals("1W",regular.getFrequency());
        Assertions.assertEquals("Testing",regular.getDescription());
        Assertions.assertEquals(AdjustmentType.AT_FORWARD,regular.getWeekendAdj());
        Assertions.assertEquals(LocalDate.of(2019, Month.APRIL,3),regular.getStart());
        Assertions.assertNull(regular.getLastDate());
    }

    @Test
    void testAccountCompare() {
        AccountDTO account = new AccountDTO();
        account.setId("FLIP");

        AccountDTO account2 = new AccountDTO();
        account2.setId("flip");

        Assertions.assertEquals(account,account2);

        AccountDTO account3 = new AccountDTO();
        account3.setId("FLOP");

        Assertions.assertEquals(-6, account.compareTo(account3));
        Assertions.assertEquals(6, account3.compareTo(account));

        Assertions.assertEquals(account2, account);
        Assertions.assertNotEquals(account3, account);
        Assertions.assertEquals(account.hashCode(),account2.hashCode());
        Assertions.assertEquals("FLIP [null]", account.toString());
    }

    @Test
    void StatusTest() {
        StatusDTO status = new StatusDTO();
        status.setStatus("FAILED");
        Assertions.assertEquals("FAILED", status.getStatus());
    }

    @Test
    void propertyTest() {
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setArchiveEnabled(true);
        applicationProperties.setReportEnabled(true);
        applicationProperties.setRegularEnabled(true);
        applicationProperties.setArchiveSchedule("Test");
        applicationProperties.setRegularSchedule("Test");
        applicationProperties.setReportSchedule("Test");
        applicationProperties.setReportShare("Test");
        applicationProperties.setReportWorking("Test");
        applicationProperties.setServiceName("Test");
        Assertions.assertEquals("Test", applicationProperties.getArchiveSchedule());
        Assertions.assertEquals("Test", applicationProperties.getRegularSchedule());
        Assertions.assertEquals("Test", applicationProperties.getReportSchedule());
        Assertions.assertEquals("Test", applicationProperties.getReportShare());
        Assertions.assertEquals("Test", applicationProperties.getReportWorking());
        Assertions.assertEquals("Test", applicationProperties.getServiceName());
        Assertions.assertTrue(applicationProperties.getArchiveEnabled());
        Assertions.assertTrue(applicationProperties.getReportEnabled());
        Assertions.assertTrue(applicationProperties.getRegularEnabled());
    }

    @Test
    void lockStatementRequest() {
        StatementIdDTO statementId = new StatementIdDTO("AMEX",3,2021);
        Assertions.assertEquals("AMEX", statementId.getAccountId());
        Assertions.assertEquals(2021, statementId.getYear().intValue());
        Assertions.assertEquals(3, statementId.getMonth().intValue());
        statementId.setMonth(32);
    }

    @Test
    void TransactionToReconciliationData() {
        TransactionDTO transaction = new TransactionDTO();
        transaction.setDescription("Test");
        transaction.setDate(utilityMapper.map(LocalDate.of(2022, Month.OCTOBER,13),String.class));
        transaction.setAmount(BigDecimal.valueOf(29.2));
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");

        ReconciliationData reconciliation = transactionMapper.map(transaction,ReconciliationData.class);
        Assertions.assertEquals("Test", reconciliation.getDescription());
        Assertions.assertEquals(29.2, reconciliation.getAmount().doubleValue(), 0.01);
        Assertions.assertEquals(LocalDate.of(2022, Month.OCTOBER,13), reconciliation.getDate());
        Assertions.assertEquals("HSE", reconciliation.getCategory().getId());
    }

    @Test
    void DateRangeDTO() {
        DateRangeDTO dateRangeDTO = new DateRangeDTO("2010-05-03","2010-06-21");

        DateRange dateRange = utilityMapper.map(dateRangeDTO, DateRange.class);
        Assertions.assertEquals(LocalDate.of(2010, Month.MAY,3), dateRange.getFrom());
        Assertions.assertEquals(LocalDate.of(2010, Month.JUNE,21), dateRange.getTo());

        dateRangeDTO = new DateRangeDTO(null,"2010-06-21");
        dateRange = utilityMapper.map(dateRangeDTO, DateRange.class);
        Assertions.assertEquals(LocalDate.of(2010, Month.JUNE,21), dateRange.getTo());

        dateRangeDTO = new DateRangeDTO("2010-05-03",null);
        dateRange = utilityMapper.map(dateRangeDTO, DateRange.class);
        Assertions.assertEquals(LocalDate.of(2010, Month.MAY,3), dateRange.getFrom());
    }

    @Test
    void ArchiveOrReportRequestDTO() {
        ArchiveOrReportRequestDTO archiveOrReportRequest = new ArchiveOrReportRequestDTO(2010,5);
        Assertions.assertEquals(2010, archiveOrReportRequest.getYear());
        Assertions.assertEquals(5, archiveOrReportRequest.getMonth());
    }

    @Test
    void ReconcileUpdateDTO() {
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(1);
        reconcileUpdate.setType("Blah");
        reconcileUpdate.setCategoryId("FSE");
        Assertions.assertEquals(1, reconcileUpdate.getId());
        Assertions.assertEquals("Blah", reconcileUpdate.getType());
        Assertions.assertEquals("FSE", reconcileUpdate.getCategoryId());
    }

    @Test
    void TestReconcileFormat() {
        ReconcileFormat format = new ReconcileFormat();
        format.setDateFormat("dd-mmm-yyyy");
        format.setAmountInColumn(1);
        format.setDateColumn(2);
        format.setId("TEST");
        format.setFirstLine(2);
        format.setReverse(true);
        format.setHeaderLine("Testing");
        format.setAmountOutColumn(2);
        format.setDescriptionColumn(4);
        Assertions.assertEquals("dd-mmm-yyyy",format.getDateFormat());
        Assertions.assertEquals(1,format.getAmountInColumn().intValue());
        Assertions.assertEquals(2,format.getDateColumn().intValue());
        Assertions.assertEquals("TEST",format.getId());
        Assertions.assertEquals(2,format.getFirstLine().intValue());
        Assertions.assertTrue(format.getReverse());
        Assertions.assertEquals("Testing",format.getHeaderLine());
        Assertions.assertEquals(2,format.getAmountOutColumn().intValue());
        Assertions.assertEquals(4,format.getDescriptionColumn().intValue());
    }

    @Test
    void testFinancialAmountToBigDecimal() {
        BigDecimal test = BigDecimal.valueOf(290.20);
        FinancialAmount financialAmount = utilityMapper.map(test,FinancialAmount.class);
        Assertions.assertEquals(290.2,financialAmount.getValue().doubleValue(),0.001);

        Assertions.assertEquals(financialAmount, test);

        FinancialAmount financialAmount2 = new FinancialAmount(BigDecimal.valueOf(290.2));
        Assertions.assertEquals(financialAmount, financialAmount2);

        // Test the other way.
        FinancialAmount faTest = new FinancialAmount(test);
        BigDecimal bdTest = utilityMapper.map(faTest, BigDecimal.class);
        Assertions.assertEquals(bdTest.toString(),faTest.getValue().toString());
    }

    @Test
    void testStatementId() {
        Account account = new Account();
        account.setId("AMEX");

        StatementId statementId = new StatementId(account,2010, 1);
        StatementId previous = StatementId.getPreviousId(statementId);
        Assertions.assertEquals(2009, previous.getYear().intValue());
        Assertions.assertEquals(12, previous.getMonth().intValue());

        statementId = new StatementId(account,2010, 12);
        StatementId next = StatementId.getNextId(statementId);
        Assertions.assertEquals(2011, next.getYear().intValue());
        Assertions.assertEquals(1, next.getMonth().intValue());

        String text = next.toString();
        Assertions.assertEquals(text.hashCode(),next.hashCode());

        Assertions.assertNotEquals(statementId,next);
    }

    @Test
    void reconciliationFileTest() {
        ReconciliationFile reconciliationFile = new ReconciliationFile();
        reconciliationFile.setError("Error");
        reconciliationFile.setSize(321L);
        reconciliationFile.setName("FredFlinstone.txt");
        reconciliationFile.setLastModified(LocalDateTime.of(2023, Month.JANUARY,2,6,32,4));
        reconciliationFile.setLoaded(false);

        Account account = new Account();
        account.setId("BANK");
        reconciliationFile.setAccount(account);

        ReconciliationFileDTO reconciliationFileDTO = transactionMapper.map(reconciliationFile,ReconciliationFileDTO.class);
        Assertions.assertEquals(321L,reconciliationFileDTO.getSize(),0.1);
        Assertions.assertEquals(LocalDateTime.of(2023, Month.JANUARY,2,6,32,4),reconciliationFileDTO.getLastModified());
        Assertions.assertEquals("BANK",reconciliationFileDTO.getAccount().getId());
        Assertions.assertEquals("FredFlinstone.txt",reconciliationFileDTO.getFilename());
        Assertions.assertEquals("Error", reconciliationFileDTO.getError());
        Assertions.assertFalse(reconciliationFileDTO.getLoaded());

        reconciliationFile.setError("Another");
        reconciliationFile.setSize(921L);
        reconciliationFile.setName("BarneyRubble.txt");
        reconciliationFile.setLastModified(LocalDateTime.of(2023, Month.FEBRUARY,3,4,12,6));
        reconciliationFile.setAccount(null);

        reconciliationFileDTO = transactionMapper.map(reconciliationFile,ReconciliationFileDTO.class);
        Assertions.assertEquals(921L,reconciliationFileDTO.getSize(),0.1);
        Assertions.assertEquals(LocalDateTime.of(2023, Month.FEBRUARY,3,4,12,6),reconciliationFileDTO.getLastModified());
        Assertions.assertNull(reconciliationFileDTO.getAccount());
        Assertions.assertEquals("BarneyRubble.txt",reconciliationFileDTO.getFilename());
        Assertions.assertEquals("Another", reconciliationFileDTO.getError());

        ReconciliationFileLoadDTO fileLoad = new ReconciliationFileLoadDTO();
        fileLoad.setFilename("grep");
        Assertions.assertEquals("grep",fileLoad.getFilename());
    }

    @Test
    void testReconcileFileUpdateDTO() {
        ReconcileFileDataUpdateDTO test = new ReconcileFileDataUpdateDTO(LocalDateTime.of(2023, Month.DECEMBER,3,10,15),"/test/path");
        Assertions.assertEquals("/test/path",test.getPath());
        Assertions.assertEquals(LocalDateTime.of(2023, Month.DECEMBER,3,10,15),test.getUpdateTime());

        test.setPath("/test/path2");
        test.setUpdateTime(LocalDateTime.of(2022, Month.NOVEMBER,2,9,14));
        Assertions.assertEquals(LocalDateTime.of(2022, Month.NOVEMBER,2,9,14),test.getUpdateTime());
    }

    @Test
    void testReconciliationFileTranId() {
        Account account = new Account();
        account.setId("TEST");
        account.setClosed(false);
        account.setColour("FFFFFF");
        account.setImagePrefix("Test");
        account.setName("Testing");

        ReconciliationFile file = new ReconciliationFile();
        file.setAccount(account);
        file.setError("");
        file.setSize(100L);
        file.setName("Fred.txt");
        file.setLastModified(LocalDateTime.of(2022, Month.NOVEMBER,2,9,14));

        ReconciliationFileTransactionId id = new ReconciliationFileTransactionId();
        id.setFile(file);
        id.setLine(10);


        ReconciliationFileTransactionId id2 = new ReconciliationFileTransactionId();
        id2.setFile(file);
        id2.setLine(10);

        Assertions.assertEquals(file,id.getFile());
        Assertions.assertEquals(10,id.getLine().intValue());
        Assertions.assertEquals("Fred.txt-10",id.toString());
        Assertions.assertEquals(-531378337,id.hashCode());

        Assertions.assertEquals(id,id2);
        id2.setLine(11);
        Assertions.assertNotEquals(id,id2);
    }

    @Test
    void testConverters() {
        Account account = new Account();
        account.setId("XNYD");
        Assertions.assertEquals("XNYD",regularMapper.map(account,String.class));

        Category category = new Category();
        category.setId("EFIY");
        Assertions.assertEquals("EFIY",regularMapper.map(category,String.class));

        FinancialAmount financialAmount = new FinancialAmount(BigDecimal.valueOf(12.89));
        Assertions.assertEquals(12.89,utilityMapper.map(financialAmount,Double.class),0.01);
    }

    @Test
    void testTransactionReport() {
        AccountDTO account = new AccountDTO();
        account.setId("BBCD");

        StatementDTO statement = new StatementDTO();
        statement.setAccountId(account.getId());
        statement.setYear(2023);
        statement.setMonth(3);

        TransactionReportDTO test = new TransactionReportDTO();
        test.setBalance(new FinancialAmount(BigDecimal.valueOf(12.3)));
        test.setId(10);
        test.setOppositeId(90);
        test.setPredicted(false);
        test.setAccount(account);
        test.setFromReconciliation(false);
        test.setAmount(new FinancialAmount(BigDecimal.valueOf(84.32)));
        test.setStatement(statement);
        test.setDescription("Testing");

        Assertions.assertEquals(12.3,test.getBalance().getValue().doubleValue(),0.1);
        Assertions.assertEquals(10,test.getId().intValue());
        Assertions.assertEquals(90,test.getOppositeId().intValue());
        Assertions.assertEquals(false,test.getPredicted());
        Assertions.assertEquals("BBCD",test.getAccount().getId());
        Assertions.assertEquals(false,test.getFromReconciliation());
        Assertions.assertEquals(84.32,test.getAmount().getValue().doubleValue(),0.1);
        Assertions.assertEquals(2023,test.getStatement().getYear().intValue());
        Assertions.assertEquals(3,test.getStatement().getMonth().intValue());
        Assertions.assertEquals("Testing",test.getDescription());
    }

    @Test
    void testTransactionSort() throws IOException {
        TransactionSortDTO test = new TransactionSortDTO();
        test.setField(TransactionSortField.CATEGORY);
        test.setType(TransactionSortType.DESCENDING);

        Assertions.assertEquals(TransactionSortField.CATEGORY,test.getField());
        Assertions.assertEquals(TransactionSortType.DESCENDING,test.getType());

        ObjectMapper objectMapper = new ObjectMapper();

        test = objectMapper.readValue("{\"field\":\"AMOUNT\",\"type\":\"ASCENDING\"}", TransactionSortDTO.class);
        Assertions.assertEquals(TransactionSortField.AMOUNT,test.getField());
        Assertions.assertEquals(TransactionSortType.ASCENDING,test.getType());
    }

    @Test
    void testDateRange2() {
        DateRangeDTO dateRange = new DateRangeDTO();

        try {
            dateRange.setTo("1929-01-32");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Date Range: the dates must be in the format yyyy-MM-dd", e.getMessage());
        }

        try {
            dateRange.setFrom("2022-05-21");
            dateRange.setTo("2022-05-19");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Date Range: the to date MUST be after the from date.",e.getMessage());
        }

        try {
            dateRange.setFrom("1929-01-32");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Date Range: the dates must be in the format yyyy-MM-dd", e.getMessage());
        }

        try {
            dateRange.setFrom(null);
            dateRange.setTo("2022-05-19");
            dateRange.setFrom("2022-05-21");
        } catch (IllegalArgumentException e) {
            Assertions.assertEquals("Date Range: the from date MUST be before the to date.",e.getMessage());
        }
    }

    @Test
    void testFilterDTO() throws UpdateDeleteAccountException, UpdateDeleteCategoryException {
        TransactionFilterDTO test = new TransactionFilterDTO();

        // Check the defaults.
        Assertions.assertEquals(600,test.getMaxPageSize().intValue());
        Assertions.assertEquals(0,test.getPageNumber().intValue());
        Assertions.assertEquals(4,test.getTransactionSorts().size());

        // Check get and sets.
        test.setPageNumber(3);
        test.setMaxPageSize(602);
        test.setFromReconciled(true);
        test.setAccounts(Collections.singletonList(this.accountManager.getExternal("BANK")));
        test.setCategories(Collections.singletonList(this.categoryManager.getExternal("HSE")));
        test.setFromReconciled(true);
        test.setValueRange(new ValueRangeDTO());
        test.setLocked(true);
        test.setPredicted(true);
        test.setStatementDate(new StatementDateDTO());
        test.setDateRange(new DateRangeDTO());
        test.setDescription("Hello");

        Assertions.assertEquals(3,test.getPageNumber().intValue());
        Assertions.assertEquals(602,test.getMaxPageSize().intValue());
        Assertions.assertTrue(test.getFromReconciled());
        Assertions.assertEquals(1,test.getAccounts().size());
        Assertions.assertEquals(1,test.getCategories().size());
        Assertions.assertTrue(test.getFromReconciled());
        Assertions.assertNull(test.getValueRange().getMaximum());
        Assertions.assertTrue(test.getLocked());
        Assertions.assertTrue(test.getPredicted());
        Assertions.assertEquals("Hello",test.getDescription());
        Assertions.assertNull(test.getStatementDate().getMonth());
        Assertions.assertNull(test.getDateRange().getTo());
    }
}
