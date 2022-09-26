package com.jbr.middletier.money;

import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.reporting.EmailGenerator;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ResourceLoader;

import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmailTest {
    @Test
    public void testEmail() throws Exception {
        CategoryRepository categoryRepository = Mockito.mock(CategoryRepository.class);
        TransactionRepository transactionRepository = Mockito.mock(TransactionRepository.class);
        StatementRepository statementRepository = Mockito.mock(StatementRepository.class);
        AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
        ResourceLoader resourceLoader = Mockito.mock(ResourceLoader.class);
        TransportWrapper transportWrapper = Mockito.mock(TransportWrapper.class);

        EmailGenerator emailGenerator = new EmailGenerator(
                transactionRepository,
                statementRepository,
                categoryRepository,
                accountRepository,
                resourceLoader,
                transportWrapper);

        //emailGenerator.generateReport("a@b.com", "a@b.com", "user", "host", "pwd", 1);
    }

    @Test
    public void test() throws ParseException {
        FinancialAmount start = new FinancialAmount(-10.02);
        FinancialAmount end = new FinancialAmount(103.02);
        List<TransactionDTO> transactions = new ArrayList<>();

        AccountDTO account = new AccountDTO();
        account.setId("BANK");

        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        category.setName("House");

        TransactionDTO transaction = new TransactionDTO();
        transaction.setId(1);
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(192.92);
        transaction.setDate(LocalDate.of(2021,1,3));
        transaction.setAccount(account);
        transactions.add(transaction);

        transaction = new TransactionDTO();
        transaction.setId(1);
        transaction.setDescription("Test");
        transaction.setCategory(category);
        transaction.setAmount(-312.92);
        transaction.setDate(LocalDate.of(2021,1,12));
        transaction.setAccount(account);
        transactions.add(transaction);

        HyperTextMarkupLanguage email = new EmailHtml(start,transactions,end);
        String f = email.getHtmlAsString();

        System.out.println(f);
    }
}
