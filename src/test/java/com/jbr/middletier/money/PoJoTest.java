package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.*;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.schedule.AdjustmentType;
import com.jbr.middletier.money.util.DateRange;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.utils.UtilityMapper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class PoJoTest {
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
    private ReconciliationMapper reconciliationMapper;

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private StatementRepository statementRepository;

    @Test
    public void accountToDTO() {
        Account account = new Account();
        account.setId("CHEESE");
        account.setColour("BLACK");
        account.setImagePrefix("Cheese");
        account.setName("Testing");
        AccountDTO accountDTO = accountMapper.map(account, AccountDTO.class);
        Assert.assertEquals("CHEESE", accountDTO.getId());
        Assert.assertEquals("BLACK",accountDTO.getColour());
        Assert.assertEquals("Testing",accountDTO.getName());
        Assert.assertEquals("Cheese",accountDTO.getImagePrefix());

        // Compare to non-accountDTO should always be false;
        @SuppressWarnings("EqualsBetweenInconvertibleTypes")
        boolean test = accountDTO.equals("Test");
        Assert.assertFalse(test);
    }

    @Test
    public void accountFromDTO() {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("HOPE");
        accountDTO.setColour("BLUE");
        accountDTO.setImagePrefix("Cheese");
        accountDTO.setName("Testing");
        Account account = accountMapper.map(accountDTO,Account.class);
        Assert.assertEquals("HOPE", account.getId());
        Assert.assertEquals("BLUE",account.getColour());
        Assert.assertEquals("Testing",account.getName());
        Assert.assertEquals("Cheese",account.getImagePrefix());
    }

    @Test
    public void categoryToDTO() {
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
        Assert.assertEquals("HOTEL",categoryDTO.getId());
        Assert.assertEquals("WHITE",categoryDTO.getColour());
        Assert.assertEquals("Test",categoryDTO.getName());
        Assert.assertTrue(categoryDTO.getExpense());
        Assert.assertEquals("GRP",categoryDTO.getGroup());
        Assert.assertTrue(categoryDTO.getRestricted());
        Assert.assertEquals(100L,categoryDTO.getSort().longValue());
        Assert.assertTrue(categoryDTO.getSystemUse());
    }

    @Test
    public void categoryFromDTO() {
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
        Assert.assertEquals("AROSE",category.getId());
        Assert.assertEquals("PINK",category.getColour());
        Assert.assertEquals("Test",category.getName());
        Assert.assertTrue(category.getExpense());
        Assert.assertEquals("GRP",category.getGroup());
        Assert.assertTrue(category.getRestricted());
        Assert.assertEquals(100L,category.getSort().longValue());
        Assert.assertTrue(category.getSystemUse());
    }

    @Test
    public void statementIdToDTO() {
        Account account = new Account();
        account.setId("FLIP");

        StatementId statementId = new StatementId();
        statementId.setAccount(account);
        statementId.setMonth(10);
        statementId.setYear(2003);

        StatementIdDTO statementIdDTO = statementMapper.map(statementId,StatementIdDTO.class);
        Assert.assertEquals("FLIP",statementIdDTO.getAccountId());
        Assert.assertEquals(10,statementIdDTO.getMonth().intValue());
        Assert.assertEquals(2003,statementIdDTO.getYear().intValue());
    }

    public void statementIdFromDTO() {
        StatementIdDTO statementIdDTO = new StatementIdDTO("BANK", 7, 2019);
        StatementId statementId = statementMapper.map(statementIdDTO,StatementId.class);
        Assert.assertEquals("BANK",statementId.getAccount().getId());
        Assert.assertEquals(7,statementId.getMonth().intValue());
        Assert.assertEquals(2019,statementId.getYear().intValue());
    }

    @Test
    public void compareStatementIdDTO() {
        StatementIdDTO lhs = new StatementIdDTO("BANK",5, 2011);
        Assert.assertEquals(0,lhs.compareTo(new StatementIdDTO("bank",5,2011)));
        Assert.assertEquals(-31,lhs.compareTo(new StatementIdDTO("a", 5, 2011)));
        Assert.assertEquals(1,lhs.compareTo(new StatementIdDTO("bank", 4, 2011)));
        Assert.assertEquals(1,lhs.compareTo(new StatementIdDTO("bank", 5, 2010)));
        Assert.assertEquals(-33,lhs.compareTo(new StatementIdDTO("clown", 5, 2011)));
        Assert.assertEquals(-1,lhs.compareTo(new StatementIdDTO("bank", 6, 2011)));
        Assert.assertEquals(-1,lhs.compareTo(new StatementIdDTO("bank", 5, 2012)));

        Assert.assertEquals(lhs, new StatementIdDTO("bank", 5, 2011));

        Assert.assertEquals(lhs.hashCode(),new StatementIdDTO("bank",5,2011).hashCode());

        Assert.assertEquals("BANK.201105", lhs.toString());
    }

    @Test
    public void compareStatementId() {
        Account account1 = new Account();
        account1.setId("BANK");

        Account account2 = new Account();
        account2.setId("a");

        Account account3 = new Account();
        account3.setId("clown");

        StatementId lhs = new StatementId(account1,2011, 5);
        Assert.assertEquals(lhs, new StatementId(account1,2011,5));
        Assert.assertNotEquals(lhs, new StatementId(account2, 2011, 5));
        Assert.assertNotEquals(lhs, new StatementId(account1, 2011, 4));
        Assert.assertNotEquals(lhs, new StatementId(account1, 2010, 5));
        Assert.assertNotEquals(lhs, new StatementId(account3, 2011, 5));
        Assert.assertNotEquals(lhs, new StatementId(account1, 2011, 6));
        Assert.assertNotEquals(lhs, new StatementId(account1, 2012, 5));

        Account account1a = new Account();
        account1a.setId("bank");

        Assert.assertEquals(lhs.hashCode(),new StatementId(account1a,2011,5).hashCode());

        Assert.assertEquals("BANK201105", lhs.toString());
    }

    @Test
    public void statementToDTO() {
        Account account = new Account();
        account.setId("BARCLAY");
        Statement statement = new Statement(account,1,2022,101.23,true);
        StatementDTO statementDTO = statementMapper.map(statement,StatementDTO.class);
        Assert.assertEquals("BARCLAY",statementDTO.getAccountId());
        Assert.assertEquals(1,statementDTO.getMonth().intValue());
        Assert.assertEquals(2022,statementDTO.getYear().intValue());
        Assert.assertTrue(statementDTO.getLocked());
        Assert.assertEquals(101.23,statementDTO.getOpenBalance(),0.001);
    }

    @Test
    public void statementFromDTO() {
        StatementDTO statementDTO = new StatementDTO();
        statementDTO.setAccountId("BANK");
        statementDTO.setMonth(2);
        statementDTO.setYear(2021);
        statementDTO.setLocked(true);
        statementDTO.setOpenBalance(102.12);
        Statement statement = statementMapper.map(statementDTO,Statement.class);
        Assert.assertEquals("BANK",statement.getId().getAccount().getId());
        Assert.assertEquals(2,statement.getId().getMonth().intValue());
        Assert.assertEquals(2021,statement.getId().getYear().intValue());
        Assert.assertTrue(statement.getLocked());
        Assert.assertEquals(102.12,statement.getOpenBalance().getValue(),0.001);
    }

    @Test
    public void transactionToDTO() {
        Account account = new Account();
        account.setId("FLIP");
        Category category = new Category();
        category.setId("FLOP");
        Statement statement = new Statement(account,1,2022,101.23,true);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(statement);
        transaction.setOppositeTransactionId(92);
        transaction.setAmount(1.29);
        transaction.setDescription("Testing");
        transaction.setDate(LocalDate.of(2018,10,7));
        TransactionDTO transactionDTO = transactionMapper.map(transaction, TransactionDTO.class);
        Assert.assertEquals("FLIP",transactionDTO.getAccountId());
        Assert.assertEquals("FLOP",transactionDTO.getCategoryId());
        Assert.assertEquals(2022,transactionDTO.getStatementYear().intValue());
        Assert.assertEquals(1,transactionDTO.getStatementMonth().intValue());
        Assert.assertEquals(92,transactionDTO.getOppositeTransactionId().intValue());
        Assert.assertEquals(1.29,transactionDTO.getAmount(),0.001);
        Assert.assertEquals("Testing",transactionDTO.getDescription());
        Assert.assertEquals("2018-10-07",transactionDTO.getDate());
        Assert.assertTrue(transactionDTO.getHasStatement());
        Assert.assertTrue(transactionDTO.getStatementLocked());
    }

    @Test
    public void transactionToDTO2() {
        Account account = new Account();
        account.setId("FLIP");
        Category category = new Category();
        category.setId("FLOP");
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(null);
        transaction.setOppositeTransactionId(92);
        transaction.setAmount(1.29);
        transaction.setDescription("Testing");
        transaction.setDate(LocalDate.of(2018,10,7));
        TransactionDTO transactionDTO = transactionMapper.map(transaction, TransactionDTO.class);
        Assert.assertEquals("FLIP",transactionDTO.getAccountId());
        Assert.assertEquals("FLOP",transactionDTO.getCategoryId());
        Assert.assertNull(transactionDTO.getStatementYear());
        Assert.assertNull(transactionDTO.getStatementMonth());
        Assert.assertEquals(92,transactionDTO.getOppositeTransactionId().intValue());
        Assert.assertEquals(1.29,transactionDTO.getAmount(),0.001);
        Assert.assertEquals("Testing",transactionDTO.getDescription());
        Assert.assertEquals("2018-10-07",transactionDTO.getDate());
        Assert.assertFalse(transactionDTO.getHasStatement());
        Assert.assertFalse(transactionDTO.getStatementLocked());
    }

    @Test
    public void transactionFromDTO() throws UpdateDeleteAccountException {
        Account account = accountManager.get("BANK");

        Statement testStatement = new Statement();
        StatementId testStatementId = new StatementId();
        testStatementId.setAccount(account);
        testStatementId.setMonth(8);
        testStatementId.setYear(2021);
        testStatement.setId(testStatementId);
        testStatement.setOpenBalance(0);
        testStatement.setLocked(false);

        statementRepository.save(testStatement);

        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAccountId("BANK");
        transactionDTO.setCategoryId("HSE");
        transactionDTO.setStatementMonth(8);
        transactionDTO.setStatementYear(2021);
        transactionDTO.setOppositeTransactionId(92);
        transactionDTO.setAmount(1.29);
        transactionDTO.setDescription("Testing");
        transactionDTO.setDate("2018-07-23");
        Transaction transaction = transactionMapper.map(transactionDTO, Transaction.class);
        Assert.assertEquals("BANK",transaction.getAccount().getId());
        Assert.assertEquals("HSE",transaction.getCategory().getId());
        Assert.assertEquals("BANK",transaction.getStatement().getId().getAccount().getId());
        Assert.assertEquals(2021,transaction.getStatement().getId().getYear().intValue());
        Assert.assertEquals(8,transaction.getStatement().getId().getMonth().intValue());
        Assert.assertEquals(92,transaction.getOppositeTransactionId().intValue());
        Assert.assertEquals(1.29,transaction.getAmount().getValue(),0.001);
        Assert.assertEquals("Testing",transaction.getDescription());
        Assert.assertEquals(LocalDate.of(2018,7,23),transaction.getDate());

        statementRepository.delete(testStatement);
    }

    @Test
    public void RegularToDTO() {
        Account account = new Account();
        account.setId("123");
        Category category = new Category();
        category.setId("456");
        Regular regular = new Regular();
        regular.setAccount(account);
        regular.setCategory(category);
        regular.setAmount(10.20);
        regular.setFrequency("1W");
        regular.setDescription("Testing");
        regular.setStart(LocalDate.of(2019,2,5));
        regular.setLastDate(LocalDate.of(2019,3,5));
        regular.setWeekendAdj(AdjustmentType.AT_BACKWARD);
        RegularDTO regularDTO = regularMapper.map(regular,RegularDTO.class);
        Assert.assertEquals("123",regularDTO.getAccountId());
        Assert.assertEquals("456",regularDTO.getCategoryId());
        Assert.assertEquals(10.20,regularDTO.getAmount(),0.001);
        Assert.assertEquals("1W",regularDTO.getFrequency());
        Assert.assertEquals("Testing",regularDTO.getDescription());
        Assert.assertEquals(AdjustmentType.AT_BACKWARD.toString(),regularDTO.getWeekendAdj());
        Assert.assertEquals("2019-02-05",regularDTO.getStart());
        Assert.assertEquals("2019-03-05",regularDTO.getLastDate());
    }

    @Test
    public void RegularToDTO2() {
        Account account = new Account();
        account.setId("123");
        Category category = new Category();
        category.setId("456");
        Regular regular = new Regular();
        regular.setAccount(account);
        regular.setCategory(category);
        regular.setAmount(10.20);
        regular.setFrequency("1W");
        regular.setDescription("Testing");
        regular.setStart(LocalDate.of(2019,2,5));
        regular.setWeekendAdj(AdjustmentType.AT_BACKWARD);
        RegularDTO regularDTO = regularMapper.map(regular,RegularDTO.class);
        Assert.assertEquals("123",regularDTO.getAccountId());
        Assert.assertEquals("456",regularDTO.getCategoryId());
        Assert.assertEquals(10.20,regularDTO.getAmount(),0.001);
        Assert.assertEquals("1W",regularDTO.getFrequency());
        Assert.assertEquals("Testing",regularDTO.getDescription());
        Assert.assertEquals(AdjustmentType.AT_BACKWARD.toString(),regularDTO.getWeekendAdj());
        Assert.assertEquals("2019-02-05",regularDTO.getStart());
        Assert.assertNull(regularDTO.getLastDate());
    }

    @Test
    public void RegularFromDTO()  {
        RegularDTO regularDTO = new RegularDTO();
        regularDTO.setAccountId("BANK");
        regularDTO.setCategoryId("FDG");
        regularDTO.setAmount(10.20);
        regularDTO.setFrequency("1W");
        regularDTO.setDescription("Testing");
        regularDTO.setStart("2019-04-03");
        regularDTO.setLastDate("2019-05-10");
        regularDTO.setWeekendAdj("FW");
        Regular regular = regularMapper.map(regularDTO,Regular.class);
        Assert.assertEquals("BANK",regular.getAccount().getId());
        Assert.assertEquals("FDG",regular.getCategory().getId());
        Assert.assertEquals(10.20,regular.getAmount(),0.001);
        Assert.assertEquals("1W",regular.getFrequency());
        Assert.assertEquals("Testing",regular.getDescription());
        Assert.assertEquals(AdjustmentType.AT_FORWARD,regular.getWeekendAdj());
        Assert.assertEquals(LocalDate.of(2019,4,3),regular.getStart());
        Assert.assertEquals(LocalDate.of(2019,5,10),regular.getLastDate());
    }

    @Test
    public void RegularFromDTO2()  {
        RegularDTO regularDTO = new RegularDTO();
        regularDTO.setAccountId("BANK");
        regularDTO.setCategoryId("FDG");
        regularDTO.setAmount(10.20);
        regularDTO.setFrequency("1W");
        regularDTO.setDescription("Testing");
        regularDTO.setStart("2019-04-03");
        regularDTO.setWeekendAdj("FW");
        Regular regular = regularMapper.map(regularDTO,Regular.class);
        Assert.assertEquals("BANK",regular.getAccount().getId());
        Assert.assertEquals("FDG",regular.getCategory().getId());
        Assert.assertEquals(10.20,regular.getAmount(),0.001);
        Assert.assertEquals("1W",regular.getFrequency());
        Assert.assertEquals("Testing",regular.getDescription());
        Assert.assertEquals(AdjustmentType.AT_FORWARD,regular.getWeekendAdj());
        Assert.assertEquals(LocalDate.of(2019,4,3),regular.getStart());
        Assert.assertNull(regular.getLastDate());
    }

    @Test
    public void testAccountCompare() {
        AccountDTO account = new AccountDTO();
        account.setId("FLIP");

        AccountDTO account2 = new AccountDTO();
        account2.setId("flip");

        Assert.assertEquals(account,account2);

        AccountDTO account3 = new AccountDTO();
        account3.setId("FLOP");

        Assert.assertEquals(-6, account.compareTo(account3));
        Assert.assertEquals(6, account3.compareTo(account));

        Assert.assertEquals(account2, account);
        Assert.assertNotEquals(account3, account);
        Assert.assertEquals(account.hashCode(),account2.hashCode());
        Assert.assertEquals("FLIP [null]", account.toString());
    }

    @Test
    public void StatusTest() {
        StatusDTO status = new StatusDTO();
        status.setStatus("FAILED");
        Assert.assertEquals("FAILED", status.getStatus());
    }

    @Test
    public void propertyTest() {
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
        Assert.assertEquals("Test", applicationProperties.getArchiveSchedule());
        Assert.assertEquals("Test", applicationProperties.getRegularSchedule());
        Assert.assertEquals("Test", applicationProperties.getReportSchedule());
        Assert.assertEquals("Test", applicationProperties.getReportShare());
        Assert.assertEquals("Test", applicationProperties.getReportWorking());
        Assert.assertEquals("Test", applicationProperties.getServiceName());
        Assert.assertTrue(applicationProperties.getArchiveEnabled());
        Assert.assertTrue(applicationProperties.getReportEnabled());
        Assert.assertTrue(applicationProperties.getRegularEnabled());
    }

    @Test
    public void lockStatementRequest() {
        StatementIdDTO statementId = new StatementIdDTO("AMEX",3,2021);
        Assert.assertEquals("AMEX", statementId.getAccountId());
        Assert.assertEquals(2021, statementId.getYear().intValue());
        Assert.assertEquals(3, statementId.getMonth().intValue());
        statementId.setMonth(32);
    }

    @Test
    public void TransactionToReconciliationData() {
        TransactionDTO transaction = new TransactionDTO();
        transaction.setDescription("Test");
        transaction.setDate(utilityMapper.map(LocalDate.of(2022,10,13),String.class));
        transaction.setAmount(29.2);
        transaction.setAccountId("AMEX");
        transaction.setCategoryId("HSE");

        ReconciliationData reconciliation = transactionMapper.map(transaction,ReconciliationData.class);
        Assert.assertEquals("Test", reconciliation.getDescription());
        Assert.assertEquals(29.2, reconciliation.getAmount(), 0.01);
        Assert.assertEquals(LocalDate.of(2022,10,13), reconciliation.getDate());
        Assert.assertEquals("HSE", reconciliation.getCategory().getId());
    }

    @Test
    public void DateRangeDTO() {
        DateRangeDTO dateRangeDTO = new DateRangeDTO("2010-05-03","2010-06-21");

        DateRange dateRange = utilityMapper.map(dateRangeDTO, DateRange.class);
        Assert.assertEquals(LocalDate.of(2010,5,3), dateRange.getFrom());
        Assert.assertEquals(LocalDate.of(2010,6,21), dateRange.getTo());
    }

    @Test
    public void ArchiveOrReportRequestDTO() {
        ArchiveOrReportRequestDTO archiveOrReportRequest = new ArchiveOrReportRequestDTO(2010,5);
        Assert.assertEquals(2010, archiveOrReportRequest.getYear());
        Assert.assertEquals(5, archiveOrReportRequest.getMonth());
    }

    @Test
    public void ReconcileUpdateDTO() {
        ReconcileUpdateDTO reconcileUpdate = new ReconcileUpdateDTO();
        reconcileUpdate.setId(1);
        reconcileUpdate.setType("Blah");
        reconcileUpdate.setCategoryId("FSE");
        Assert.assertEquals(1, reconcileUpdate.getId());
        Assert.assertEquals("Blah", reconcileUpdate.getType());
        Assert.assertEquals("FSE", reconcileUpdate.getCategoryId());
    }

    @Test
    public void TestReconcileFormat() {
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
        Assert.assertEquals("dd-mmm-yyyy",format.getDateFormat());
        Assert.assertEquals(1,format.getAmountInColumn().intValue());
        Assert.assertEquals(2,format.getDateColumn().intValue());
        Assert.assertEquals("TEST",format.getId());
        Assert.assertEquals(2,format.getFirstLine().intValue());
        Assert.assertTrue(format.getReverse());
        Assert.assertEquals("Testing",format.getHeaderLine());
        Assert.assertEquals(2,format.getAmountOutColumn().intValue());
        Assert.assertEquals(4,format.getDescriptionColumn().intValue());
    }

    @Test
    public void testFinancialAmountToDouble() {
        Double test = 290.2;
        FinancialAmount financialAmount = utilityMapper.map(test,FinancialAmount.class);
        Assert.assertEquals(290.2,financialAmount.getValue(),0.001);
    }

    @Test
    public void testStatementId() {
        Account account = new Account();
        account.setId("AMEX");

        StatementId statementId = new StatementId(account,2010, 1);
        StatementId previous = StatementId.getPreviousId(statementId);
        Assert.assertEquals(2009, previous.getYear().intValue());
        Assert.assertEquals(12, previous.getMonth().intValue());

        statementId = new StatementId(account,2010, 12);
        StatementId next = StatementId.getNextId(statementId);
        Assert.assertEquals(2011, next.getYear().intValue());
        Assert.assertEquals(1, next.getMonth().intValue());

        String text = next.toString();
        Assert.assertEquals(text.hashCode(),next.hashCode());

        Assert.assertNotEquals(statementId,next);
    }

    @Test
    public void matchDataToDTO() {
        Account account = new Account();
        account.setId("WHAT");
        Category category = new Category();
        category.setId("WHERE");
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.of(2019,2,18));
        transaction.setDescription("Test");
        transaction.setAmount(32.09);
        transaction.setStatement(null);
        transaction.setAccount(account);
        transaction.setCategory(category);
        MatchData source = new MatchData(transaction);
        source.setAfterAmount(0.29);
        source.setBeforeAmount(102.02);

        MatchDataDTO matchData = reconciliationMapper.map(source,MatchDataDTO.class);
        Assert.assertEquals(-1,matchData.getId());
        Assert.assertEquals(0.29,matchData.getAfterAmount(),0.001);
        Assert.assertEquals(102.02,matchData.getBeforeAmount(), 0.01);
        Assert.assertNotNull(matchData.getTransaction());
        Assert.assertFalse(matchData.getTransaction().getHasStatement());
        Assert.assertFalse(matchData.getTransaction().getStatementLocked());
        Assert.assertEquals("2019-02-18",matchData.getReconciliationDate());
        Assert.assertEquals(32.09,matchData.getTransaction().getAmount(),0.001);
        Assert.assertEquals("WHAT", matchData.getAccountId());
        Assert.assertEquals("WHERE", matchData.getCategoryId());
        Assert.assertEquals("Test",matchData.getDescription());
    }

    @Test
    public void matchDataToDTO2() {
        Account account = new Account();
        account.setId("WHAT");
        Category category = new Category();
        category.setId("WHERE");
        StatementId statementId = new StatementId(account,2019,2);
        Statement statement = new Statement();
        statement.setId(statementId);
        statement.setLocked(false);
        statement.setOpenBalance(0.21);
        Transaction transaction = new Transaction();
        transaction.setDate(LocalDate.of(2019,2,18));
        transaction.setDescription("Test");
        transaction.setAmount(32.09);
        transaction.setStatement(statement);
        transaction.setAccount(account);
        transaction.setCategory(category);
        MatchData source = new MatchData(transaction);
        source.setAfterAmount(0.29);
        source.setBeforeAmount(102.02);

        MatchDataDTO matchData = reconciliationMapper.map(source,MatchDataDTO.class);
        Assert.assertEquals(-1,matchData.getId());
        Assert.assertEquals(0.29,matchData.getAfterAmount(),0.001);
        Assert.assertEquals(102.02,matchData.getBeforeAmount(), 0.01);
        Assert.assertNotNull(matchData.getTransaction());
        Assert.assertTrue(matchData.getTransaction().getHasStatement());
        Assert.assertFalse(matchData.getTransaction().getStatementLocked());
        Assert.assertEquals("2019-02-18",matchData.getReconciliationDate());
        Assert.assertEquals(32.09,matchData.getTransaction().getAmount(),0.001);
        Assert.assertEquals("WHAT", matchData.getAccountId());
        Assert.assertEquals("WHERE", matchData.getCategoryId());
        Assert.assertEquals("Test",matchData.getDescription());
    }

    @Test
    public void matchDataToDTO3() {
        ReconciliationData reconcilationData = new ReconciliationData();
        reconcilationData.setAmount(20.21);
        reconcilationData.setDescription("Testing");
        reconcilationData.setDate(LocalDate.of(2018,2,15));
        reconcilationData.setCategory(null);
        Account account = new Account();
        account.setId("WHAT");
        MatchData source = new MatchData(reconcilationData,account);
        source.setAfterAmount(0.74);
        source.setBeforeAmount(112.72);

        MatchDataDTO matchData = reconciliationMapper.map(source,MatchDataDTO.class);
        Assert.assertEquals(0,matchData.getId());
        Assert.assertEquals(0.74,matchData.getAfterAmount(),0.001);
        Assert.assertEquals(112.72,matchData.getBeforeAmount(), 0.01);
        Assert.assertNull(matchData.getTransaction());
        Assert.assertEquals("2018-02-15",matchData.getReconciliationDate());
        Assert.assertEquals(20.21,matchData.getReconciliationAmount(),0.001);
        Assert.assertEquals("WHAT", matchData.getAccountId());
        Assert.assertNull(matchData.getCategoryId());
        Assert.assertEquals("Testing",matchData.getDescription());
    }
}
