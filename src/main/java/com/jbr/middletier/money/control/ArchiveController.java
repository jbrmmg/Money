package com.jbr.middletier.money.control;

import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jbr.middletier.money.data.ArchiveRequest;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.manage.WebLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

@Controller
@RequestMapping("/jbr")
public class ArchiveController {
    final static private Logger LOG = LoggerFactory.getLogger(ArchiveController.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final WebLogManager webLogManager;

    @Autowired
    public ArchiveController(TransactionRepository transactionRepository,
                            StatementRepository statementRepository,
                            WebLogManager webLogManager ) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.webLogManager = webLogManager;
    }

    @RequestMapping(path="/int/money/transaction/archive", method= RequestMethod.POST)
    public @ResponseBody
    ArchiveRequest  archive(@RequestBody ArchiveRequest archiveRequest) {
        try {
            LOG.info("Archive Controller - request archive.");

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

            archiveRequest.setStatus("OK");
        } catch (Exception ex) {
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to archive " + ex);
            archiveRequest.setStatus("FAILED");
        }

        return archiveRequest;
    }
}
