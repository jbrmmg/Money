package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.*;
import com.jbr.middletier.money.utils.CssAssertHelper;
import com.jbr.middletier.money.utils.HtmlTableAssertHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
@WebAppConfiguration
public class AnnualReportTest extends Support {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private ModelMapper modelMapper;

    private void cleanUp() {
        transactionRepository.deleteAll();
        for(Statement next : statementRepository.findAll()) {
            if(next.getLocked()) {
                next.setLocked(false);
                statementRepository.save(next);
            }
        }
    }

    @Test
    @Ignore("HTML generation needs checking")
    public void testReport() throws Exception {
        cleanUp();

        // Clear the directories.
        deleteDirectoryContents(new File(applicationProperties.getReportWorking()).toPath());
        deleteDirectoryContents(new File(applicationProperties.getReportShare()).toPath());

        // Create some transactions
        // TODO add transactions so that pie can be checked.

        AccountDTO account = new AccountDTO();
        account.setId("AMEX");
        CategoryDTO category = new CategoryDTO();
        category.setId("HSE");
        TransactionDTO transaction = new TransactionDTO();
        transaction.setAccount(account);
        transaction.setCategory(category);
        transaction.setDate(LocalDate.of(2010,1,1));
        transaction.setAmount(10.02);
        transaction.setDescription("Testing");

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
        StatementIdDTO lockStatementRequest = new StatementIdDTO();
        lockStatementRequest.setAccount(account);
        lockStatementRequest.setMonth(1);
        lockStatementRequest.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/statement/lock")
                        .content(this.json(lockStatementRequest))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Request the report.
        ArchiveOrReportRequestDTO request = new ArchiveOrReportRequestDTO();
        request.setMonth(1);
        request.setYear(2010);

        getMockMvc().perform(post("/jbr/int/money/transaction/annualreport")
                        .content(this.json(request))
                        .contentType(getContentType()))
                .andExpect(status().isOk());

        // Check that the report exists.
        File htmlFile = new File(applicationProperties.getReportWorking() + "/Report.html");
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/AMEX.svg").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.png").toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportWorking() + "/HSE.svg").toPath()));
        Assert.assertTrue(Files.exists(htmlFile.toPath()));
        Assert.assertTrue(Files.exists(new File(applicationProperties.getReportShare() + "/2010/Report-2010.pdf").toPath()));

        // Check the HTML file.
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        String html = new String(Files.readAllBytes(htmlFile.toPath()));
        is.setCharacterStream(new StringReader(html));
        org.w3c.dom.Document  document = db.parse(is);

        Document domDocument = new DOMBuilder().build(document);
        Element root = domDocument.getRootElement();

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
        Assert.assertEquals(13, headers.size());
        Assert.assertEquals("2010 Summary", headers.get(0).getText());
        Assert.assertEquals("January 2010", headers.get(1).getText());
        Assert.assertEquals("February 2010", headers.get(2).getText());
        Assert.assertEquals("March 2010", headers.get(3).getText());
        Assert.assertEquals("April 2010", headers.get(4).getText());
        Assert.assertEquals("May 2010", headers.get(5).getText());
        Assert.assertEquals("June 2010", headers.get(6).getText());
        Assert.assertEquals("July 2010", headers.get(7).getText());
        Assert.assertEquals("August 2010", headers.get(8).getText());
        Assert.assertEquals("September 2010", headers.get(9).getText());
        Assert.assertEquals("October 2010", headers.get(10).getText());
        Assert.assertEquals("November 2010", headers.get(11).getText());
        Assert.assertEquals("December 2010", headers.get(12).getText());

        List<Element> images = body.getChildren("img");
        Assert.assertEquals(13, images.size());
        for(Element next : images) {
            Assert.assertEquals("pie", next.getAttribute("class").getValue());
            Assert.assertEquals("400px", next.getAttribute("height").getValue());
            Assert.assertEquals("400px", next.getAttribute("width").getValue());
            Assert.assertTrue(next.getAttribute("src").getValue().contains("pie-"));
        }

        List<Element> tables = body.getChildren("table");
        Assert.assertEquals(13, tables.size());

        List<List<HtmlTableAssertHelper.HtmlTableAssertHelperData>> expected = new ArrayList<>();
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Current Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Previous Year", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Change in Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"House", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"10.02","amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"0.00", "amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"Total", "total-row");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"10.02", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"0.00", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableAssertHelper.checkTable(tables.get(0),expected);

        expected = new ArrayList<>();
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Current Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Previous Month", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Change in Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"House", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"10.02","amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"0.00", "amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"Total", "total-row");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"10.02", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"0.00", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableAssertHelper.checkTable(tables.get(1),expected);

        expected = new ArrayList<>();
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Current Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Previous Month", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Change in Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderImage(expected, 1,25, 25, "./target/testfiles/PdnReport/HSE.png");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"House", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"0.00","amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"10.02", "amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"-100.00%", "amount amount-debit");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"Total", "total-row");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"0.00", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"10.02", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 2,"-100.00%", "total-row amount amount-debit");
        HtmlTableAssertHelper.checkTable(tables.get(2),expected);

        expected = new ArrayList<>();
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Current Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Previous Month", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 0,"Change in Spend", "total-column");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"", "");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"Total", "total-row");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"0.00", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"0.00", "total-row amount");
        HtmlTableAssertHelper.expectTableBuliderText(expected, 1,"", "");
        HtmlTableAssertHelper.checkTable(tables.get(3),expected);
        HtmlTableAssertHelper.checkTable(tables.get(4),expected);
        HtmlTableAssertHelper.checkTable(tables.get(5),expected);
        HtmlTableAssertHelper.checkTable(tables.get(6),expected);
        HtmlTableAssertHelper.checkTable(tables.get(7),expected);
        HtmlTableAssertHelper.checkTable(tables.get(8),expected);
        HtmlTableAssertHelper.checkTable(tables.get(9),expected);
        HtmlTableAssertHelper.checkTable(tables.get(10),expected);
        HtmlTableAssertHelper.checkTable(tables.get(11),expected);
        HtmlTableAssertHelper.checkTable(tables.get(12),expected);

        List<Element> paragraphs = body.getChildren("p");
        Assert.assertEquals(12,  paragraphs.size());
        for(Element next : paragraphs) {
            Assert.assertEquals("page-break-after: always;", next.getAttribute("style").getValue());
        }
    }
}
