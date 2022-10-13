package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dto.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.time.LocalDate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class PoJoTest {
    @Autowired
    private ModelMapper modelMapper;

    @Test
    public void accountToDTO() {
        Account account = new Account();
        account.setId("XXXX");
        account.setColour("FCFCFC");
        account.setImagePrefix("Cheese");
        account.setName("Testing");
        AccountDTO accountDTO = modelMapper.map(account, AccountDTO.class);
        Assert.assertEquals("XXXX", accountDTO.getId());
        Assert.assertEquals("FCFCFC",accountDTO.getColour());
        Assert.assertEquals("Testing",accountDTO.getName());
        Assert.assertEquals("Cheese",accountDTO.getImagePrefix());
    }

    @Test
    public void accountFromDTO() {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("XXXX");
        accountDTO.setColour("FCFCFC");
        accountDTO.setImagePrefix("Cheese");
        accountDTO.setName("Testing");
        Account account = modelMapper.map(accountDTO,Account.class);
        Assert.assertEquals("XXXX", account.getId());
        Assert.assertEquals("FCFCFC",account.getColour());
        Assert.assertEquals("Testing",account.getName());
        Assert.assertEquals("Cheese",account.getImagePrefix());
    }

    @Test
    public void categoryToDTO() {
        Category category = new Category();
        category.setId("XYZ");
        category.setColour("FAFAFA");
        category.setName("Test");
        category.setExpense(true);
        category.setGroup("GRP");
        category.setRestricted(true);
        category.setSort(100L);
        category.setSystemUse(true);
        CategoryDTO categoryDTO = modelMapper.map(category, CategoryDTO.class);
        Assert.assertEquals("XYZ",categoryDTO.getId());
        Assert.assertEquals("FAFAFA",categoryDTO.getColour());
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
        categoryDTO.setId("XYZ");
        categoryDTO.setColour("FAFAFA");
        categoryDTO.setName("Test");
        categoryDTO.setExpense(true);
        categoryDTO.setGroup("GRP");
        categoryDTO.setRestricted(true);
        categoryDTO.setSort(100L);
        categoryDTO.setSystemUse(true);
        Category category = modelMapper.map(categoryDTO, Category.class);
        Assert.assertEquals("XYZ",category.getId());
        Assert.assertEquals("FAFAFA",category.getColour());
        Assert.assertEquals("Test",category.getName());
        Assert.assertTrue(category.getExpense());
        Assert.assertEquals("GRP",category.getGroup());
        Assert.assertTrue(category.getRestricted());
        Assert.assertEquals(100L,category.getSort().longValue());
        Assert.assertTrue(category.getSystemUse());
    }

    @Test
    public void statementToDTO() {
        Account account = new Account();
        account.setId("XXXX");
        Statement statement = new Statement(account,1,2022,101.23,true);
        StatementDTO statementDTO = modelMapper.map(statement,StatementDTO.class);
        Assert.assertEquals("XXXX",statementDTO.getId().getAccount().getId());
        Assert.assertEquals(1,statementDTO.getId().getMonth().intValue());
        Assert.assertEquals(2022,statementDTO.getId().getYear().intValue());
        Assert.assertTrue(statementDTO.getLocked());
        Assert.assertEquals(101.23,statementDTO.getOpenBalance(),0.001);
    }

    @Test
    public void statementFromDTO() {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("XXXY");
        StatementDTO statementDTO = new StatementDTO();
        StatementIdDTO statementIdDTO = new StatementIdDTO();
        statementIdDTO.setAccount(accountDTO);
        statementIdDTO.setMonth(2);
        statementIdDTO.setYear(2021);
        statementDTO.setId(statementIdDTO);
        statementDTO.setLocked(true);
        statementDTO.setOpenBalance(102.12);
        Statement statement = modelMapper.map(statementDTO,Statement.class);
        Assert.assertEquals("XXXY",statement.getId().getAccount().getId());
        Assert.assertEquals(2,statement.getId().getMonth().intValue());
        Assert.assertEquals(2021,statement.getId().getYear().intValue());
        Assert.assertTrue(statement.getLocked());
        Assert.assertEquals(102.12,statement.getOpenBalance().getValue(),0.001);

        StatementIdDTO statementIdDTO2 = new StatementIdDTO();
        statementIdDTO2.setAccount(accountDTO);
        statementIdDTO2.setMonth(2);
        statementIdDTO2.setYear(2021);
        Assert.assertEquals(0, statementIdDTO.compareTo(statementIdDTO2));

        statementIdDTO2.setYear(2020);
        Assert.assertEquals(1, statementIdDTO.compareTo(statementIdDTO2));

        //noinspection EqualsBetweenInconvertibleTypes,EqualsReplaceableByObjectsCall,UnnecessaryBoxing
        Assert.assertNotEquals(true, statementIdDTO.equals(Double.valueOf(21.2)));

        StatementDTO statementDTO2 = new StatementDTO();
        statementDTO2.setId(statementIdDTO2);
        statementIdDTO2.setYear(2021);
        statementIdDTO2.setMonth(2);

        //noinspection SimplifiableAssertion
        Assert.assertEquals(true, statementDTO.equals(statementDTO2));
        //noinspection SimplifiableAssertion
        Assert.assertEquals(true, statementDTO.getId().equals(statementDTO2.getId()));

        statementIdDTO2.setMonth(3);
        Assert.assertNotEquals(true, statementDTO.equals(statementDTO2));
        Assert.assertNotEquals(true, statementDTO.getId().equals(statementDTO2.getId()));

        //noinspection EqualsBetweenInconvertibleTypes,EqualsReplaceableByObjectsCall,UnnecessaryBoxing
        Assert.assertNotEquals(true, statementDTO.equals(Double.valueOf(21.2)));
    }

    @Test
    public void transactionToDTO() {
        Account account = new Account();
        account.setId("XXXW");
        Category category = new Category();
        category.setId("XYZ");
        Statement statement = new Statement(account,1,2022,101.23,true);
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setStatement(statement);
        transaction.setOppositeTransactionId(92);
        transaction.setAmount(1.29);
        transaction.setDescription("Testing");
        TransactionDTO transactionDTO = modelMapper.map(transaction, TransactionDTO.class);
        Assert.assertEquals("XXXW",transactionDTO.getAccount().getId());
        Assert.assertEquals("XYZ",transactionDTO.getCategory().getId());
        Assert.assertEquals("XXXW",transactionDTO.getStatement().getId().getAccount().getId());
        Assert.assertEquals(2022,transactionDTO.getStatement().getId().getYear().intValue());
        Assert.assertEquals(1,transactionDTO.getStatement().getId().getMonth().intValue());
        Assert.assertEquals(92,transactionDTO.getOppositeTransactionId().intValue());
        Assert.assertEquals(1.29,transactionDTO.getAmount(),0.001);
        Assert.assertEquals("Testing",transactionDTO.getDescription());
    }

    @Test
    public void transactionFromDTO() {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("XXXW");
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId("XYZ");
        StatementDTO statementDTO = new StatementDTO();
        StatementIdDTO statementIdDTO = new StatementIdDTO();
        statementIdDTO.setAccount(accountDTO);
        statementIdDTO.setYear(2021);
        statementIdDTO.setMonth(8);
        statementDTO.setId(statementIdDTO);
        TransactionDTO transactionDTO = new TransactionDTO();
        transactionDTO.setAccount(accountDTO);
        transactionDTO.setCategory(categoryDTO);
        transactionDTO.setStatement(statementDTO);
        transactionDTO.setOppositeTransactionId(92);
        transactionDTO.setAmount(1.29);
        transactionDTO.setDescription("Testing");
        Transaction transaction = modelMapper.map(transactionDTO, Transaction.class);
        Assert.assertEquals("XXXW",transaction.getAccount().getId());
        Assert.assertEquals("XYZ",transaction.getCategory().getId());
        Assert.assertEquals("XXXW",transaction.getStatement().getId().getAccount().getId());
        Assert.assertEquals(2021,transaction.getStatement().getId().getYear().intValue());
        Assert.assertEquals(8,transaction.getStatement().getId().getMonth().intValue());
        Assert.assertEquals(92,transaction.getOppositeTransactionId().intValue());
        Assert.assertEquals(1.29,transaction.getAmount().getValue(),0.001);
        Assert.assertEquals("Testing",transaction.getDescription());
    }

    @Test
    public void RegularToDTO() throws ParseException {
        Account account = new Account();
        account.setId("XXXF");
        Category category = new Category();
        category.setId("XHF");
        Regular regular = new Regular();
        regular.setAccount(account);
        regular.setCategory(category);
        regular.setAmount(10.20);
        regular.setFrequency("1W");
        regular.setDescription("Testing");
        regular.setStart(LocalDate.of(2019,2,5));
        regular.setLastDate(LocalDate.of(2019,3,5));
        RegularDTO regularDTO = modelMapper.map(regular,RegularDTO.class);
        Assert.assertEquals("XXXF",regularDTO.getAccount().getId());
        Assert.assertEquals("XHF",regularDTO.getCategory().getId());
        Assert.assertEquals(10.20,regularDTO.getAmount(),0.001);
        Assert.assertEquals("1W",regularDTO.getFrequency());
        Assert.assertEquals("Testing",regularDTO.getDescription());
        Assert.assertEquals(LocalDate.of(2019,2,5),regularDTO.getStart());
        Assert.assertEquals(LocalDate.of(2019,3,5),regularDTO.getLastDate());
    }

    @Test
    public void RegularFromDTO()  {
        AccountDTO accountDTO = new AccountDTO();
        accountDTO.setId("XXXF");
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId("XHF");
        RegularDTO regularDTO = new RegularDTO();
        regularDTO.setAccount(accountDTO);
        regularDTO.setCategory(categoryDTO);
        regularDTO.setAmount(10.20);
        regularDTO.setFrequency("1W");
        regularDTO.setDescription("Testing");
        regularDTO.setStart(LocalDate.of(2019,2,5));
        regularDTO.setLastDate(LocalDate.of(2019,3,5));
        Regular regular = modelMapper.map(regularDTO,Regular.class);
        Assert.assertEquals("XXXF",regular.getAccount().getId());
        Assert.assertEquals("XHF",regular.getCategory().getId());
        Assert.assertEquals(10.20,regular.getAmount(),0.001);
        Assert.assertEquals("1W",regular.getFrequency());
        Assert.assertEquals("Testing",regular.getDescription());
        Assert.assertEquals(LocalDate.of(2019,2,5),regular.getStart());
        Assert.assertEquals(LocalDate.of(2019,3,5),regular.getLastDate());

        //noinspection SimplifiableAssertion,EqualsBetweenInconvertibleTypes,UnnecessaryBoxing
        Assert.assertFalse(accountDTO.equals(Double.valueOf(210.2)));
    }

    @Test
    public void testAccountCompare() {
        AccountDTO account = new AccountDTO();
        account.setId("ACFE");

        AccountDTO account2 = new AccountDTO();
        account2.setId("ACFE");

        Assert.assertEquals(account,account2);

        AccountDTO account3 = new AccountDTO();
        account3.setId("BCFE");

        Assert.assertEquals(-1, account.compareTo(account3));
        Assert.assertEquals(1, account3.compareTo(account));

        //noinspection SimplifiableAssertion
        Assert.assertTrue(account.equals(account2));
        //noinspection SimplifiableAssertion
        Assert.assertFalse(account.equals(account3));

        Assert.assertEquals(account.hashCode(),account2.hashCode());

        Assert.assertEquals("ACFE [null]", account.toString());
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
        StatementIdDTO statementId = new StatementIdDTO();
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        statementId.setAccount(account);
        statementId.setYear(2021);
        statementId.setMonth(3);
        Assert.assertEquals("AMEX", statementId.getAccount().getId());
        Assert.assertEquals(2021, statementId.getYear().intValue());
        Assert.assertEquals(3, statementId.getMonth().intValue());
        statementId.setMonth(32);
    }

    @Test
    public void TransactionToReconciliationData() {
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");

        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");

        TransactionDTO transaction = new TransactionDTO();
        transaction.setDescription("Test");
        transaction.setDate(LocalDate.of(2022,10,13));
        transaction.setAmount(29.2);
        transaction.setAccount(account);
        transaction.setCategory(category);

        ReconciliationData reconciliation = modelMapper.map(transaction,ReconciliationData.class);
        Assert.assertEquals("Test", reconciliation.getDescription());
        Assert.assertEquals(29.2, reconciliation.getAmount(), 0.01);
        Assert.assertEquals(LocalDate.of(2022,10,13), reconciliation.getDate());
        Assert.assertEquals("HSE", reconciliation.getCategory().getId());
    }
}
