package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import com.jbr.middletier.money.manager.TransactionFilter;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.schedule.AdjustmentType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import java.time.LocalDate;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TransactionReportTest {
    @Autowired
    public TransactionMapper transactionMapper;

    @Autowired
    public TransactionFilter filter;

    @Autowired
    public ApplicationProperties applicationProperties;

    private Account createAccount() {
        Account testAccount = new Account();
        testAccount.setId("TEST");
        testAccount.setName("Testing");
        testAccount.setColour("FFFFFF");
        testAccount.setClosed(false);
        testAccount.setImagePrefix("Blah");

        return testAccount;
    }

    private Category createCategory() {
        Category testCategory = new Category();
        testCategory.setId("TEST");
        testCategory.setName("Testing");
        testCategory.setColour("FFFFFF");
        testCategory.setExpense(false);
        testCategory.setGroup("BLH");
        testCategory.setRestricted(false);
        testCategory.setSort(100L);
        testCategory.setSystemUse(false);

        return testCategory;
    }

    private Transaction createTestTransaction() {
        Account testAccount = createAccount();

        StatementId testStatementId = new StatementId();
        testStatementId.setAccount(testAccount);
        testStatementId.setYear(2023);
        testStatementId.setMonth(3);

        Statement testStatement = new Statement();
        testStatement.setId(testStatementId);
        testStatement.setLocked(false);
        testStatement.setOpenBalance(100);

        Transaction result = new Transaction();
        result.setAmount(12.93);
        result.setDate(LocalDate.of(2023,10,14));
        result.setDescription("Testing");
        result.setOppositeTransactionId(73);
        result.setAccount(testAccount);
        result.setCategory(createCategory());
        result.setStatement(testStatement);

        return result;
    }

    private Regular createRegular() {
        Regular result = new Regular();
        result.setAccount(createAccount());
        result.setAmount(-112.92);
        result.setCategory(createCategory());
        result.setId(1);
        result.setDescription("Test");
        result.setFrequency("1M");
        result.setLastDate(LocalDate.of(2023,11,1));
        result.setStart(LocalDate.of(2023,11,1));
        result.setWeekendAdj(AdjustmentType.AT_FORWARD);

        return result;
    }

    private MatchData createMatchWithTransaction() {
        return new MatchData(createTestTransaction());
    }

    private MatchData createMatch(boolean withNulls) {
        ReconciliationData reconciliationData = new ReconciliationData();
        reconciliationData.setAmount(-291.21);
        reconciliationData.setDate(LocalDate.of(2023,9, 12));
        if(!withNulls) {
            reconciliationData.setDescription("Test Reconciliation");
            reconciliationData.setCategory(createCategory());
        }

        return new MatchData(reconciliationData,createAccount());
    }

    private DateRangeDTO getRegularFilterDateRange(Regular regular) throws CannotDetermineNextDateException {
        LocalDate from = regular.getNextDate(applicationProperties.getToday()).minusDays(1);
        LocalDate to = regular.getNextDate(applicationProperties.getToday()).plusDays(1);
        return new DateRangeDTO(Constants.MONEY_DATE_FORMATTER.format(from),Constants.MONEY_DATE_FORMATTER.format(to));
    }

    @Test
    public void testMapperFromRegular() throws CannotDetermineNextDateException {
        // Test mapping a transaction to a report transaction.
        Regular test = createRegular();
        TransactionReportDTO dto = transactionMapper.map(test,TransactionReportDTO.class);

        // Main test.
        Assert.assertNotNull(dto);
        Assert.assertEquals(test.getAmount(), dto.getAmount().getValue(), 0.001);
        Assert.assertNull(dto.getId());
        Assert.assertTrue(dto.getPredicted());
        Assert.assertFalse(dto.getFromReconciliation());
        Assert.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getNextDate(applicationProperties.getToday())), dto.getDate());
        Assert.assertEquals(test.getDescription(), dto.getDescription());
        Assert.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assert.assertEquals(test.getCategory().getId(), dto.getCategory().getId());
        Assert.assertNull(dto.getStatement());

        // Check that it still works when contained objects are
        test.setDescription(null);
        test.setAccount(null);
        test.setCategory(null);
        dto = transactionMapper.map(test,TransactionReportDTO.class);

        Assert.assertNull(dto.getStatement());
        Assert.assertNull(dto.getAccount());
        Assert.assertNull(dto.getCategory());
        Assert.assertNull(dto.getDescription());
    }

    @Test
    public void testMapperFromTransaction() {
        // Test mapping a transaction to a report transaction.
        Transaction test = createTestTransaction();
        TransactionReportDTO dto = transactionMapper.map(test,TransactionReportDTO.class);

        // Main test.
        Assert.assertNotNull(dto);
        Assert.assertEquals(test.getAmount().getValue(), dto.getAmount().getValue(), 0.001);
        Assert.assertEquals(0, dto.getId().intValue());
        Assert.assertFalse(dto.getPredicted());
        Assert.assertFalse(dto.getFromReconciliation());
        Assert.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assert.assertEquals(test.getDescription(), dto.getDescription());
        Assert.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assert.assertEquals(test.getCategory().getId(), dto.getCategory().getId());
        Assert.assertEquals(test.getStatement().getId().getAccount().getId(),dto.getStatement().getAccountId());
        Assert.assertEquals(test.getStatement().getId().getMonth(),dto.getStatement().getMonth());

        // Check that it still works when contained objects are
        test.setStatement(null);
        test.setDescription(null);
        test.setAccount(null);
        test.setCategory(null);
        dto = transactionMapper.map(test,TransactionReportDTO.class);

        Assert.assertNull(dto.getStatement());
        Assert.assertNull(dto.getAccount());
        Assert.assertNull(dto.getCategory());
        Assert.assertNull(dto.getDescription());
    }

    @Test
    public void testMapperFromMatch() {
        // Test mapping a transaction to a report transaction.
        MatchData test = createMatchWithTransaction();
        TransactionReportDTO dto = transactionMapper.map(test,TransactionReportDTO.class);

        // Main test.
        Assert.assertNotNull(dto);
        Assert.assertEquals(test.getAmount(), dto.getAmount().getValue(), 0.001);
        Assert.assertEquals(0, dto.getId().intValue());
        Assert.assertFalse(dto.getPredicted());
        Assert.assertTrue(dto.getFromReconciliation());
        Assert.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assert.assertEquals(test.getDescription(), dto.getDescription());
        Assert.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assert.assertEquals(test.getCategory().getId(), dto.getCategory().getId());

        // Check its ok when created from reconciliation data.
        test = createMatch(false);
        dto = transactionMapper.map(test,TransactionReportDTO.class);

        Assert.assertNull(dto.getStatement());
        Assert.assertNotNull(dto);
        Assert.assertEquals(test.getAmount(), dto.getAmount().getValue(), 0.001);
        Assert.assertNull(dto.getId());
        Assert.assertFalse(dto.getPredicted());
        Assert.assertTrue(dto.getFromReconciliation());
        Assert.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assert.assertEquals(test.getDescription(), dto.getDescription());
        Assert.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assert.assertEquals(test.getCategory().getId(), dto.getCategory().getId());

        // Check that it still works when contained objects are
        test = createMatch(true);
        dto = transactionMapper.map(test,TransactionReportDTO.class);
        Assert.assertNull(dto.getStatement());
        Assert.assertNull(dto.getCategory());
        Assert.assertNull(dto.getDescription());
    }

    @Test
    public void testFilterAmount() {
        // Check a transaction matches a filter
        Transaction test = createTestTransaction();
        test.setAmount(-13.48);

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setValueRange(new ValueRangeDTO(-32,-10));

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(-32.1);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(-9.98);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(12.81);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(-10.1);
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(-31.9);
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        Regular testRegular = createRegular();
        dto.setValueRange(new ValueRangeDTO(-113,-110));
        Assert.assertTrue(this.filter.passTransaction(testRegular,dto).isPresent());

        MatchData testMatch = createMatchWithTransaction();
        dto.setValueRange(new ValueRangeDTO(10,14));
        Assert.assertTrue(this.filter.passTransaction(testMatch,dto).isPresent());
    }

    @Test
    public void testFilterDate() throws CannotDetermineNextDateException {
        // Check a transaction matches a filter (14-10-2023)
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setDateRange(new DateRangeDTO("2023-10-01","2023-10-31"));

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setDate(LocalDate.of(2023,11,1));
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        test.setDate(LocalDate.of(2023,9,30));
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        test.setDate(LocalDate.of(2023,10,1));
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setDate(LocalDate.of(2023,10,31));
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        // Check date of a regular payment.
        Regular testRegular = createRegular();
        dto.setDateRange(getRegularFilterDateRange(testRegular));
        Assert.assertTrue(this.filter.passTransaction(testRegular,dto).isPresent());

        // Check date of match data.
        MatchData testMatch = createMatch(false);
        dto.setDateRange(new DateRangeDTO("2023-09-03","2023-09-15"));
        Assert.assertTrue(this.filter.passTransaction(testMatch,dto).isPresent());
    }

    @Test
    public void testFilterAccount() {
        // Check a transaction matches a filter (TEST)
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();

        AccountDTO account = new AccountDTO();
        account.setId("TEST");

        dto.setAccounts(Stream.of(account).toList());

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        Account transactionAccount = new Account();
        transactionAccount.setId("TXST");
        test.setAccount(transactionAccount);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        // Check regular.
        Regular testRegular = createRegular();
        Assert.assertTrue(this.filter.passTransaction(testRegular,dto).isPresent());

        testRegular.setAccount(transactionAccount);
        Assert.assertFalse(this.filter.passTransaction(testRegular,dto).isPresent());

        // Check match data.
        MatchData matchData = createMatchWithTransaction();
        Assert.assertTrue(this.filter.passTransaction(matchData,dto).isPresent());
    }

    @Test
    public void testFilterCategory() {
        // Check a transaction matches a filter (TEST)
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();

        CategoryDTO category = new CategoryDTO();
        category.setId("TEST");

        dto.setCategories(Stream.of(category).toList());

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        Category transactionCategory = new Category();
        transactionCategory.setId("TXST");
        test.setCategory(transactionCategory);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());

        // Check regular.
        Regular testRegular = createRegular();
        Assert.assertTrue(this.filter.passTransaction(testRegular,dto).isPresent());

        testRegular.setCategory(transactionCategory);
        Assert.assertFalse(this.filter.passTransaction(testRegular,dto).isPresent());

        // Check reconciliation
        MatchData testMatch = createMatch(false);
        Assert.assertTrue(this.filter.passTransaction(testMatch,dto).isPresent());
    }

    @Test
    public void testFilterStatement() {
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();
        StatementDateDTO statementDate = new StatementDateDTO();
        statementDate.setYear(2023);
        statementDate.setMonth(3);
        dto.setStatementDate(statementDate);

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilterLocked() {
        Transaction test = createTestTransaction();
        test.getStatement().setLocked(true);

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setLocked(true);

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.getStatement().setLocked(false);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilterPredicted() {
        Regular regular = createRegular();

        Transaction test = createTestTransaction();
        test.getStatement().setLocked(true);

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setPredicted(true);

        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());
        Assert.assertTrue(this.filter.passTransaction(regular,dto).isPresent());

        dto.setPredicted(false);
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
        Assert.assertFalse(this.filter.passTransaction(regular,dto).isPresent());
    }

    @Test
    public void testFilterReconciled() {
        Regular regular = createRegular();

        Transaction test = createTestTransaction();
        test.getStatement().setLocked(true);

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setFromReconciled(true);
        dto.setReconciliationAccount("BANK");

        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());
        Assert.assertFalse(this.filter.passTransaction(regular,dto).isPresent());

        dto.setReconciliationAccount("");
        dto.setFromReconciled(false);
        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
        Assert.assertTrue(this.filter.passTransaction(regular,dto).isPresent());
    }

    @Test
    public void testFilterAll() {
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();
        StatementDateDTO statementDate = new StatementDateDTO();
        statementDate.setYear(2023);
        statementDate.setMonth(3);
        dto.setStatementDate(statementDate);

        AccountDTO account = new AccountDTO();
        account.setId("TEST");
        dto.setAccounts(Stream.of(account).toList());

        CategoryDTO category = new CategoryDTO();
        category.setId("TEST");
        dto.setCategories(Stream.of(category).toList());

        dto.setDateRange(new DateRangeDTO("2023-10-01","2023-10-31"));

        dto.setValueRange(new ValueRangeDTO(11,14));

        dto.setLocked(false);

        dto.setPredicted(false);

        dto.setReconciliationAccount("");

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilterAllRegular() throws CannotDetermineNextDateException {
        Regular test = createRegular();

        TransactionFilterDTO dto = new TransactionFilterDTO();

        AccountDTO account = new AccountDTO();
        account.setId("TEST");
        dto.setAccounts(Stream.of(account).toList());

        CategoryDTO category = new CategoryDTO();
        category.setId("TEST");
        dto.setCategories(Stream.of(category).toList());

        dto.setDateRange(getRegularFilterDateRange(test));

        dto.setValueRange(new ValueRangeDTO(-114,-110));

        dto.setLocked(false);

        dto.setPredicted(true);

        dto.setReconciliationAccount("");

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilterAllMatch() {
        MatchData test = createMatchWithTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();

        AccountDTO account = new AccountDTO();
        account.setId("TEST");
        dto.setAccounts(Stream.of(account).toList());

        CategoryDTO category = new CategoryDTO();
        category.setId("TEST");
        dto.setCategories(Stream.of(category).toList());

        dto.setDateRange(new DateRangeDTO("2023-09-01","2023-10-31"));

        dto.setValueRange(new ValueRangeDTO(10,14));

        dto.setLocked(false);

        dto.setPredicted(false);

        dto.setReconciliationAccount("TEST");

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());
    }
}
