package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.AccountDTO;
import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.dto.CategoryDTO;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.utils.CssAssertHelper;
import com.jbr.middletier.money.utils.HtmlTableHelper;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import com.jbr.middletier.money.xml.html.ReportHtml;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
@ActiveProfiles(value="report")
public class ReportTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ModelMapper modelMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();
        statementRepository.deleteAll();

        for(Account next : accountRepository.findAll()) {
            Statement statement = new Statement();
            statement.setId(new StatementId(next,2010,1));
            statement.setLocked(false);
            statement.setOpenBalance(0);

            statementRepository.save(statement);
        }
    }

    private List<Transaction> standardReportTestData() throws IOException {
        // Clean up
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Setup a list of transactions with the bare minimum required.
        Account account = new Account();
        account.setId("AMEX");
        Category category = new Category();
        category.setId("HSE");
        category.setName("House");

        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,1));
        transaction.setAmount(-10.02);
        transaction.setDescription("Testing");
        transactions.add(transaction);

        transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(-210.02);
        transaction.setDescription("Testing 1");
        transactions.add(transaction);

        category = new Category();
        category.setId("FDG");
        category.setName("Grocery");
        transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(-84.12);
        transaction.setDescription("This is a much longer description test!!");
        transactions.add(transaction);

        return transactions;
    }

    private Element getRoot(String html) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(html));
        org.w3c.dom.Document  document = db.parse(is);
        Document domDocument = new DOMBuilder().build(document);
        return domDocument.getRootElement();
    }

    @Test
    public void htmlReportTestHeaderAndPie() throws IOException, ParserConfigurationException, SAXException {
        List<Transaction> transactions = standardReportTestData();

        // Setup a list of previous transactions.
        List<Transaction> previousTransactions = new ArrayList<>();

        HyperTextMarkupLanguage html = new ReportHtml(transactions,
                previousTransactions,
                LocalDate.of(2010,1,1),
                applicationProperties.getReportWorking(),
                ReportHtml.ReportType.MONTH );

        // Check the html.
        Element root = getRoot(html.getHtmlAsString());

        Assert.assertEquals("html", root.getName());
        Element head = root.getChild("head");
        Assert.assertNotNull(head);

        Element title = head.getChild("title");
        Assert.assertNotNull(title);
        Assert.assertEquals("Report", title.getText());

        Element style = head.getChild("style");
        Assert.assertNotNull(style);
        CssAssertHelper.checkReportCSS(style.getValue());

        Element body = root.getChild("body");
        Assert.assertNotNull(body);

        List<Element> headers = body.getChildren("h1");
        Assert.assertEquals(1, headers.size());
        Assert.assertEquals("January 2010", headers.get(0).getText());

        List<Element> images = body.getChildren("img");
        Assert.assertEquals(1, images.size());
        Assert.assertEquals("pie", images.get(0).getAttribute("class").getValue());
        Assert.assertEquals("400px", images.get(0).getAttribute("height").getValue());
        Assert.assertEquals("400px", images.get(0).getAttribute("width").getValue());
        Assert.assertTrue(images.get(0).getAttribute("src").getValue().contains("pie-"));
    }

    @Test
    public void htmlReportTestComparisonTable() throws IOException, ParserConfigurationException, SAXException {
        List<Transaction> transactions = standardReportTestData();

        // Setup a list of previous transactions.
        List<Transaction> previousTransactions = new ArrayList<>();

        HyperTextMarkupLanguage html = new ReportHtml(transactions,
                previousTransactions,
                LocalDate.of(2010,1,1),
                applicationProperties.getReportWorking(),
                ReportHtml.ReportType.MONTH );

        // Check the html.
        Element root = getRoot(html.getHtmlAsString());
        Element body = root.getChild("body");

        List<Element> tables = body.getChildren("table");
        Assert.assertEquals(2, tables.size());

        List<List<HtmlTableHelper.HtmlTableHelperData>> expected = new ArrayList<>();
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Current Spend", "total-column");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Previous Month", "total-column");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Change in Spend", "total-column");
        HtmlTableHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/FDG.png");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"Grocery", "");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"-84.12", "amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"0.00", "amount");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"", "");
        HtmlTableHelper.expectTableBuliderImage(expected, 2,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"House", "");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"-220.04","amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"0.00", "amount");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 3,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 3,"Total", "total-row");
        HtmlTableHelper.expectTableBuliderText(expected, 3,"-304.16", "total-row amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 3,"0.00", "total-row amount");
        HtmlTableHelper.expectTableBuliderText(expected, 3,"", "");
        Assert.assertTrue(HtmlTableHelper.checkTable(tables.get(0),expected));
    }

    @Test
    public void htmlReportTestTransactionTable() throws IOException, ParserConfigurationException, SAXException {
        List<Transaction> transactions = standardReportTestData();

        // Setup a list of previous transactions.
        List<Transaction> previousTransactions = new ArrayList<>();

        HyperTextMarkupLanguage html = new ReportHtml(transactions,
                previousTransactions,
                LocalDate.of(2010,1,1),
                applicationProperties.getReportWorking(),
                ReportHtml.ReportType.MONTH );

        // Check the html.
        Element root = getRoot(html.getHtmlAsString());
        Element body = root.getChild("body");

        List<Element> tables = body.getChildren("table");
        Assert.assertEquals(2, tables.size());

        List<List<HtmlTableHelper.HtmlTableHelperData>> expected = new ArrayList<>();
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Date", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Description", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Amount", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Date", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Description", "");
        HtmlTableHelper.expectTableBuliderText(expected, 0,"Amount", "");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"01-Jan2010", "date");
        HtmlTableHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/AMEX.png");
        HtmlTableHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"Testing", "description");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"-10.02", "amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"", "center-column");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"02-Jan2010", "date");
        HtmlTableHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/AMEX.png");
        HtmlTableHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/FDG.png");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"This is a much longerdescription test!!", "description");
        HtmlTableHelper.expectTableBuliderText(expected, 1,"-84.12", "amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"02-Jan2010", "date");
        HtmlTableHelper.expectTableBuliderImage(expected, 2,25, 25, "./target/testfiles/PdnReport/AMEX.png");
        HtmlTableHelper.expectTableBuliderImage(expected, 2,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"Testing 1", "description");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"-210.02", "amount amount-debit");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "center-column");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableHelper.expectTableBuliderText(expected, 2,"", "");
        Assert.assertTrue(HtmlTableHelper.checkTable(tables.get(1),expected));
    }

    @Test
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,1));
        transaction.setAmount(-10.02);
        transaction.setDescription("Testing");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(-210.02);
        transaction.setDescription("Testing 1");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        category.setId("FDG");
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,2));
        transaction.setAmount(-84.12);
        transaction.setDescription("This is a much longer description test!!");

        getMockMvc().perform(post("/jbr/ext/money/transaction")
                        .content(this.json(Collections.singletonList(transaction)))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        StatementId statementId = new StatementId();
        statementId.setAccount(modelMapper.map(account,Account.class));
        statementId.setMonth(1);
        statementId.setYear(2010);
        Statement statement = new Statement();
        statement.setId(statementId);

        // Add the transaction to statement
        for(Transaction next : transactionRepository.findAll()) {
            next.setStatement(statement);
            transactionRepository.save(next);
        }

        // Lock the statement
        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(statementId))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/transaction/report")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the report exists.
        File htmlFile = new File(applicationProperties.getReportWorking() + "/Report.html");
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/pie-.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/pie.svg").toPath()));
        Assert.assertTrue(Files.exists(htmlFile.toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-January-2010.pdf").toPath()));

        // Check the HTML file.
        String html = new String(Files.readAllBytes(htmlFile.toPath()));
        Element root = getRoot(html);
        Element body = root.getChild("body");

        List<Element> paragraphs = body.getChildren("p");
        Assert.assertEquals(1,  paragraphs.size());
        Assert.assertEquals("page-break-after: always;", paragraphs.get(0).getAttribute("style").getValue());
    }
}
