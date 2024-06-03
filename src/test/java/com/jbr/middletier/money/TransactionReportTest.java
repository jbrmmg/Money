package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
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
}
