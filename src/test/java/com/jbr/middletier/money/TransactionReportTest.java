package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.dto.mapper.TransactionMapper;
import com.jbr.middletier.money.manager.TransactionFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class TransactionReportTest {
    @Autowired
    public TransactionMapper transactionMapper;

    @Autowired
    public TransactionFilter filter;

    private Transaction createTestTransaction() {
        Account testAccount = new Account();
        testAccount.setId("TEST");
        testAccount.setName("Testing");
        testAccount.setColour("FFFFFF");
        testAccount.setClosed(false);
        testAccount.setImagePrefix("Blah");

        Category testCategory = new Category();
        testCategory.setId("TEST");
        testCategory.setName("Testing");
        testCategory.setColour("FFFFFF");
        testCategory.setExpense(false);
        testCategory.setGroup("BLH");
        testCategory.setRestricted(false);
        testCategory.setSort(100L);
        testCategory.setSystemUse(false);

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
        result.setCategory(testCategory);
        result.setStatement(testStatement);

        return result;
    }

    @Test
    public void testMapper() {
        // Test mapping a transaction to a report transaction.
        Transaction test = createTestTransaction();
        TransactionReportDTO dto = transactionMapper.map(test,TransactionReportDTO.class);

        // Main test.
        Assert.assertNotNull(dto);
        Assert.assertEquals(test.getAmount().getValue(), dto.getAmount().getValue(), 0.001);
        Assert.assertEquals(0, dto.getId().intValue());
        Assert.assertFalse(dto.getPredicted());
        Assert.assertFalse(dto.getFromReconciliation());
        Assert.assertEquals(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(test.getDate()), dto.getDate());
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
    public void testFilter1() {
        // Check a transaction matches a filter
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setValueRange(new ValueRangeDTO(10,32,false));

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setAmount(32.1);
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilter2() {
        // Check a transaction matches a filter (14-10-2023)
        Transaction test = createTestTransaction();

        TransactionFilterDTO dto = new TransactionFilterDTO();
        dto.setDateRange(new DateRangeDTO("2023-10-01","2023-10-31"));

        Assert.assertTrue(this.filter.passTransaction(test,dto).isPresent());

        test.setDate(LocalDate.of(2023,11,14));
        Assert.assertFalse(this.filter.passTransaction(test,dto).isPresent());
    }

    @Test
    public void testFilter3() {
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
    }
}
