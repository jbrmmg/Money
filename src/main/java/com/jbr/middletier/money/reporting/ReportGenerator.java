package com.jbr.middletier.money.reporting;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.*;

@Controller
public class ReportGenerator {
    final static private Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                             StatementRepository statementRepository ) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
    }

    public void generateReport() throws IOException, DocumentException {
        LOG.info("Generate report");

        // Archive the oldest year in the database.

        // Find the oldest year in the database.
        long oldestYear = 100000;

        Iterable<Statement> years = statementRepository.findAll();
        for(Statement nextStatement: years) {
            if(nextStatement.getId().getYear() < oldestYear) {
                oldestYear = nextStatement.getId().getYear();
            }
        }

        // The year must be at least x years ago.

        // Mark all the statements in that year for deletion.

        // Mark all the transactions for deletion.

        String htmlFilename = "/home/jason/Working/test.html";
        String pdfFilename = "/home/jason/Working/test.pdf";

        File htmlFile = new File(htmlFilename);
        PrintWriter writer2 = new PrintWriter(htmlFile);
        writer2.println("<html>");
        writer2.println("<head>");
        writer2.println("<title>This is a test file.</title>");
        writer2.println("</head>");
        writer2.println("<body>");
        writer2.println("<p>This is a test.</p>");
        writer2.println("</body>");
        writer2.println("</html>");
        writer2.close();

        // Generate a pie chart of spending for that year.

        // Generate a pie chart of spending per month.

        // Generate a month on month chart.

        // Generate a list of transactions.

        // Generate a PDF?
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document,
                new FileOutputStream(pdfFilename));
        document.open();
        XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                new FileInputStream(htmlFilename));
        document.close();
    }
}
