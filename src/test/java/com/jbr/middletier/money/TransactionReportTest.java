package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.config.Constants;
import com.jbr.middletier.money.data.internal.TransactionReport;
import com.jbr.middletier.money.data.primary.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import com.jbr.middletier.money.exceptions.UpdateDeleteAccountException;
import com.jbr.middletier.money.exceptions.UpdateDeleteCategoryException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.CategoryManager;
import com.jbr.middletier.money.reconciliation.MatchData;
import com.jbr.middletier.money.schedule.AdjustmentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
class TransactionReportTest {
    @Autowired
    public TransactionMapper transactionMapper;

    @Autowired
    public ApplicationProperties applicationProperties;

    @Autowired
    public AccountManager accountManager;

    @Autowired
    public CategoryManager categoryManager;

    private Transaction createTestTransaction() throws UpdateDeleteAccountException, UpdateDeleteCategoryException {
        Account testAccount = accountManager.get("BANK");

        StatementId testStatementId = new StatementId();
        testStatementId.setAccount(testAccount);
        testStatementId.setYear(2023);
        testStatementId.setMonth(3);

        Statement testStatement = new Statement();
        testStatement.setId(testStatementId);
        testStatement.setLocked(false);
        testStatement.setOpenBalance(BigDecimal.valueOf(100));

        Transaction result = new Transaction();
        result.setAmount(BigDecimal.valueOf(12.93));
        result.setDate(LocalDate.of(2023, Month.OCTOBER,14));
        result.setDescription("Testing");
        result.setOppositeTransactionId(73);
        result.setAccount(testAccount);
        result.setCategory(categoryManager.get("HSE"));
        result.setStatement(testStatement);

        return result;
    }

    private Regular createRegular() throws UpdateDeleteAccountException, UpdateDeleteCategoryException {
        Regular result = new Regular();
        result.setAccount(accountManager.get("BANK"));
        result.setAmount(BigDecimal.valueOf(-112.92));
        result.setCategory(categoryManager.get("HSE"));
        result.setId(1);
        result.setDescription("Test");
        result.setFrequency("1M");
        result.setLastDate(LocalDate.of(2023, Month.NOVEMBER,1));
        result.setStart(LocalDate.of(2023, Month.NOVEMBER,1));
        result.setWeekendAdj(AdjustmentType.AT_FORWARD);

        return result;
    }

    private MatchData createMatchWithTransaction() throws UpdateDeleteCategoryException, UpdateDeleteAccountException {
        return new MatchData(createTestTransaction());
    }

    private MatchData createMatch(boolean withNulls) throws UpdateDeleteCategoryException, UpdateDeleteAccountException {
        ReconciliationData reconciliationData = new ReconciliationData();
        reconciliationData.setAmount(BigDecimal.valueOf(-291.21));
        reconciliationData.setDate(LocalDate.of(2023, Month.SEPTEMBER, 12));
        if(!withNulls) {
            reconciliationData.setDescription("Test Reconciliation");
            reconciliationData.setCategory(categoryManager.get("HSE"));
        }

        return new MatchData(reconciliationData,accountManager.get("BANK"));
    }

    @Test
    void testMapperFromRegular() throws CannotDetermineNextDateException, UpdateDeleteCategoryException, UpdateDeleteAccountException {
        // Test mapping a transaction to a report transaction.
        Regular test = createRegular();
        TransactionReport transactionReport = transactionMapper.map(test,TransactionReport.class);
        TransactionReportDTO dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        // Main test.
        Assertions.assertNotNull(dto);
        Assertions.assertEquals(test.getAmount().doubleValue(), dto.getAmount().getValue().doubleValue(), 0.001);
        Assertions.assertEquals(0,dto.getId().intValue());
        Assertions.assertTrue(dto.getPredicted());
        Assertions.assertFalse(dto.getFromReconciliation());
        Assertions.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getNextDate(applicationProperties.getToday())), dto.getDate());
        Assertions.assertEquals(test.getDescription(), dto.getDescription());
        Assertions.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assertions.assertEquals(test.getCategory().getId(), dto.getCategory().getId());
        Assertions.assertNull(dto.getStatement());

        // Check that it still works when contained objects are
        test.setDescription(null);
        test.setAccount(null);
        test.setCategory(null);
        transactionReport = transactionMapper.map(test,TransactionReport.class);
        dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        Assertions.assertNull(dto.getStatement());
        Assertions.assertNull(dto.getAccount());
        Assertions.assertNull(dto.getCategory());
        Assertions.assertNull(dto.getDescription());
    }

    @Test
    void testMapperFromTransaction() throws UpdateDeleteCategoryException, UpdateDeleteAccountException {
        // Test mapping a transaction to a report transaction.
        Account testAccount = new Account();
        testAccount.setId("BANK");
        Category testCategory = new Category();
        testCategory.setId("HSE");
        Statement testStatement = new Statement();
        StatementId testStatementId = new StatementId();
        testStatementId.setAccount(testAccount);
        testStatementId.setYear(2010);
        testStatementId.setMonth(1);
        testStatement.setId(testStatementId);
        testStatement.setLocked(false);
        testStatement.setOpenBalance(BigDecimal.ZERO);
        Transaction test = createTestTransaction();
        test.setAccount(testAccount);
        test.setCategory(testCategory);
        test.setStatement(testStatement);
        TransactionReport transactionReport = transactionMapper.map(test,TransactionReport.class);
        TransactionReportDTO dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        // Main test.
        Assertions.assertNotNull(dto);
        Assertions.assertEquals(test.getAmount().getValue().doubleValue(), dto.getAmount().getValue().doubleValue(), 0.001);
        Assertions.assertEquals(0, dto.getId().intValue());
        Assertions.assertFalse(dto.getPredicted());
        Assertions.assertFalse(dto.getFromReconciliation());
        Assertions.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assertions.assertEquals(test.getDescription(), dto.getDescription());
        Assertions.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assertions.assertEquals(test.getCategory().getId(), dto.getCategory().getId());
        Assertions.assertEquals(test.getStatement().getId().getAccount().getId(),dto.getStatement().getAccountId());
        Assertions.assertEquals(test.getStatement().getId().getMonth(),dto.getStatement().getMonth());

        // Check that it still works when contained objects are
        test.setStatement(null);
        test.setDescription(null);
        test.setAccount(null);
        test.setCategory(null);
        transactionReport = transactionMapper.map(test,TransactionReport.class);
        dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        Assertions.assertNull(dto.getStatement());
        Assertions.assertNull(dto.getAccount());
        Assertions.assertNull(dto.getCategory());
        Assertions.assertNull(dto.getDescription());
    }

    @Test
    void testMapperFromMatch() throws UpdateDeleteCategoryException, UpdateDeleteAccountException {
        // Test mapping a transaction to a report transaction.
        MatchData test = createMatchWithTransaction();
        TransactionReport transactionReport = transactionMapper.map(test,TransactionReport.class);
        TransactionReportDTO dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        // Main test.
        Assertions.assertNotNull(dto);
        Assertions.assertEquals(test.getAmount().doubleValue(), dto.getAmount().getValue().doubleValue(), 0.001);
        Assertions.assertEquals(0, dto.getId().intValue());
        Assertions.assertFalse(dto.getPredicted());
        Assertions.assertTrue(dto.getFromReconciliation());
        Assertions.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assertions.assertEquals(test.getDescription(), dto.getDescription());
        Assertions.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assertions.assertEquals(test.getCategory().getId(), dto.getCategory().getId());

        // Check its ok when created from reconciliation data.
        test = createMatch(false);
        transactionReport = transactionMapper.map(test,TransactionReport.class);
        dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);

        Assertions.assertNull(dto.getStatement());
        Assertions.assertNotNull(dto);
        Assertions.assertEquals(test.getAmount().doubleValue(), dto.getAmount().getValue().doubleValue(), 0.001);
        Assertions.assertEquals(0, dto.getId().intValue());
        Assertions.assertFalse(dto.getPredicted());
        Assertions.assertTrue(dto.getFromReconciliation());
        Assertions.assertEquals(Constants.MONEY_DATE_FORMATTER.format(test.getDate()), dto.getDate());
        Assertions.assertEquals(test.getDescription(), dto.getDescription());
        Assertions.assertEquals(test.getAccount().getId(), dto.getAccount().getId());
        Assertions.assertEquals(test.getCategory().getId(), dto.getCategory().getId());

        // Check that it still works when contained objects are
        test = createMatch(true);
        transactionReport = transactionMapper.map(test,TransactionReport.class);
        dto = transactionMapper.map(transactionReport,TransactionReportDTO.class);
        Assertions.assertNull(dto.getStatement());
        Assertions.assertNull(dto.getCategory());
        Assertions.assertNull(dto.getDescription());
    }
}
