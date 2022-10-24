package com.jbr.middletier.money.reporting;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.manager.LogoManager;
import com.jbr.middletier.money.xml.html.HyperTextMarkupLanguage;
import com.jbr.middletier.money.xml.html.ReportHtml;
import com.jbr.middletier.money.xml.svg.CategorySvg;
import com.jbr.middletier.money.xml.svg.PieChartSvg;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class ReportGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);

    private final TransactionRepository transactionRepository;
    private final ApplicationProperties applicationProperties;
    private final LogoManager logoManager;
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                           ApplicationProperties applicationProperties,
                           LogoManager logoManager,
                           StatementRepository statementRepository,
                           AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.applicationProperties = applicationProperties;
        this.logoManager = logoManager;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
    }

    private void createPieChart(List<Transaction> transactions,String type) throws IOException, TranscoderException {
        String pieChartFile = applicationProperties.getReportWorking() + "/pie.svg";
        try(PrintWriter pie = new PrintWriter(pieChartFile)) {
            ScalableVectorGraphics pieChart = new PieChartSvg(transactions);
            pie.write(pieChart.getSvgAsString());
        }

        createPngFromSvg(pieChartFile,applicationProperties.getReportWorking() + "/pie-" + type +".png", 1000, 1000);
    }

    private void createPngFromSvg(String svgFilename, String pngFilename, float height, float width) throws IOException, TranscoderException {
        String svgUriInput = Paths.get(svgFilename).toUri().toURL().toString();
        TranscoderInput inputSvgImage = new TranscoderInput(svgUriInput);
        //Step-2: Define OutputStream to PNG Image and attach to TranscoderOutput
        OutputStream pngOstream = Files.newOutputStream(Paths.get(pngFilename));
        TranscoderOutput outputPngImage = new TranscoderOutput(pngOstream);
        // Step-3: Create PNGTranscoder and define hints if required
        PNGTranscoder myConverter = new PNGTranscoder();
        myConverter.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH,width);
        myConverter.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT,height);
        // Step-4: Convert and Write output
        myConverter.transcode(inputSvgImage, outputPngImage);
        // Step 5- close / flush Output Stream
        pngOstream.flush();
        pngOstream.close();
    }

    private void createAccountImages(String workingDirectory, List<Transaction> transactions) throws IOException, TranscoderException {
        for(Transaction nextTransactions: transactions) {
            // Is there already a png for this account?
            File pngFile = new File(workingDirectory,nextTransactions.getAccount().getId() + ".png");

            if(!pngFile.exists()) {
                // Create an SVG for the account.
                File svgFile = new File(workingDirectory, nextTransactions.getAccount().getId() + ".svg");
                try(PrintWriter svgWriter = new PrintWriter(svgFile)) {
                    svgWriter.write(logoManager.getSvgLogoForAccount(nextTransactions.getAccount().getId(),false).getSvgAsString());
                }

                // Create a PNG from SVG
                createPngFromSvg(workingDirectory + "/" + nextTransactions.getAccount().getId() + ".svg",
                        workingDirectory + "/" + nextTransactions.getAccount().getId() + ".png",
                        100,
                        100);
            }
        }
    }

    private void createCategoryImages(String workingDirectory, List<Transaction> transactions) throws IOException, TranscoderException {
        for(Transaction nextTransactions: transactions) {
            // Is there already a png for this account?
            File pngFile = new File(workingDirectory,nextTransactions.getCategory().getId() + ".png");

            if(!pngFile.exists()) {
                // Create an SVG for the account.
                File svgFile = new File(workingDirectory, nextTransactions.getCategory().getId() + ".svg");
                try(PrintWriter svgWriter = new PrintWriter(svgFile)) {

                    ScalableVectorGraphics categorySvg = new CategorySvg(nextTransactions.getCategory());
                    svgWriter.write(categorySvg.getSvgAsString());
                }

                // Create a PNG from SVG
                createPngFromSvg(workingDirectory + "/" + nextTransactions.getCategory().getId() + ".svg",
                        workingDirectory + "/" + nextTransactions.getCategory().getId() + ".png",
                        100,
                        100);
            }
        }
    }

    private void createWorkingDirectories() {
        // Does the working directory exist?
        if(!Files.exists(Paths.get(applicationProperties.getReportWorking()))) {
            //noinspection ResultOfMethodCallIgnored
            new File(applicationProperties.getReportWorking()).mkdirs();
        }

        if(!Files.exists(Paths.get(applicationProperties.getReportShare()))) {
            //noinspection ResultOfMethodCallIgnored
            new File(applicationProperties.getReportShare()).mkdirs();
        }
    }

    private void generatePDF() throws IOException, DocumentException {
        // Generate a PDF?
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document,
                Files.newOutputStream(Paths.get(applicationProperties.getPDFFilename())));
        document.open();
        XMLWorkerHelper.getInstance().parseXHtml(writer, document,
                Files.newInputStream(Paths.get(applicationProperties.getHtmlFilename())));
        document.close();
    }

    private void createImages(List<Transaction> transactions) throws IOException, TranscoderException {
        LOG.info("Create the images for accounts.");
        createAccountImages(applicationProperties.getReportWorking(),transactions);

        LOG.info("Create the images for categories.");
        createCategoryImages(applicationProperties.getReportWorking(),transactions);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void copyFile(String source,
                          String destinationPath,
                          String destination) throws IOException {
        // Does the destination path exist?
        if(!Files.exists(Paths.get(destinationPath))) {
            new File(destinationPath).mkdirs();
        }

        Files.copy( Paths.get(source), Paths.get(destinationPath + "/" + destination), StandardCopyOption.REPLACE_EXISTING );
    }

    public void generateReport(int year, int month) throws IOException, DocumentException, TranscoderException {
        LOG.info("Generate report");

        createWorkingDirectories();
        List<Transaction> transactions = transactionRepository.findByStatementIdYearAndStatementIdMonth(year,month);

        int previousMonth = month - 1;
        int previousYear = year;

        if(month == 1) {
            previousMonth = 12;
            previousYear--;
        }
        List<Transaction> previousTransactionList = transactionRepository.findByStatementIdYearAndStatementIdMonth(previousYear,previousMonth);

        File htmlFile = new File(applicationProperties.getHtmlFilename());
        try(PrintWriter writer2 = new PrintWriter(htmlFile)) {
            HyperTextMarkupLanguage reportHtml = new ReportHtml(transactions,
                    previousTransactionList,
                    LocalDate.of(year,month,1),
                    applicationProperties.getReportWorking(),
                    ReportHtml.ReportType.MONTH);

            writer2.println(reportHtml.getHtmlAsString());
        }

        createImages(transactions);
        createImages(previousTransactionList);
        createPieChart(transactions,"");

        generatePDF();

        // Copy the report to the share.
        copyFile(applicationProperties.getPDFFilename(),
                applicationProperties.getReportShare() + "/" + year,
                getMonthFilename(false,year,month));
    }

    public void generateAnnualReport(int year) throws IOException, TranscoderException, DocumentException {
        LOG.info("Generate annual report");

        // Get all the transactions for the specified statement.
        List<Transaction> transactions = transactionRepository.findByStatementIdYear(year);
        List<Transaction> previoustransactions = transactionRepository.findByStatementIdYear(year - 1);

        createWorkingDirectories();

        File htmlFile = new File(applicationProperties.getHtmlFilename());
        try(PrintWriter writer2 = new PrintWriter(htmlFile)) {
            HyperTextMarkupLanguage reportHtml = new ReportHtml(transactions,
                    previoustransactions,
                    LocalDate.of(year,1,1),
                    applicationProperties.getReportWorking(),
                    ReportHtml.ReportType.ANNUAL);

            writer2.println(reportHtml.getHtmlAsString());
        }

        createImages(transactions);
        createImages(previoustransactions);
        createPieChart(transactions,"yr");

        for(int i = 0; i < 12; i++) {
            createPieChart(transactions, String.valueOf(i));
        }

        // Generate a PDF?
        generatePDF();

        // Copy the report to the share.
        copyFile(applicationProperties.getPDFFilename(),
                applicationProperties.getReportShare() + "/" + year,
                getYearFilename(false, year));
    }

    private String getYearFilename(boolean fullPath, long year) {
        if(!fullPath) {
            return "Report-" + year + ".pdf";
        }

        return applicationProperties.getReportShare() + "/" + year + "/Report-" + year + ".pdf";
    }

    private String getMonthFilename(boolean fullPath, int year, int month) {

        LocalDate reportDate = LocalDate.of(year, month, 1);
        String reportDateString = DateTimeFormatter.ofPattern("MMMM-yyyy").format(reportDate);

        if(!fullPath) {
            return "Report-" + reportDateString + ".pdf";
        }

        return applicationProperties.getReportShare() + "/" + year + "/Report-" + reportDateString + ".pdf";
    }

    static class MonthStatus {
        int year;
        int month;

        int lockedStatementCount;
        int statementsFound;
        int activeAccounts;
    }

    public Map<Integer,MonthStatus> getMonthStatusMap(int activeAccounts) {
        Map<Integer,MonthStatus> monthStatusMap = new HashMap<>();

        // Make sure reports have been generated where all statements are locked.
        Iterable<Statement> allStatements = statementRepository.findAll();
        for(Statement nextStatement: allStatements) {
            if(nextStatement.getLocked()) {
                // What is the ID?
                Integer statementId = (nextStatement.getId().getYear() * 100 + nextStatement.getId().getMonth());
                MonthStatus nextMonthStatus;

                if(monthStatusMap.containsKey(statementId)) {
                    nextMonthStatus = monthStatusMap.get(statementId);
                } else {
                    nextMonthStatus = new MonthStatus();
                    nextMonthStatus.month = nextStatement.getId().getMonth();
                    nextMonthStatus.year = nextStatement.getId().getYear();
                    nextMonthStatus.lockedStatementCount = 0;
                    nextMonthStatus.statementsFound = 0;
                    nextMonthStatus.activeAccounts = activeAccounts;

                    monthStatusMap.put(statementId,nextMonthStatus);
                }

                nextMonthStatus.lockedStatementCount++;
                nextMonthStatus.statementsFound++;
            }
        }

        return monthStatusMap;
    }

    @Scheduled(cron = "#{@applicationProperties.reportSchedule}")
    public void regularReport() throws DocumentException, IOException, TranscoderException {
        // If this is enabled, then generate reports.
        if(!applicationProperties.getReportEnabled()) {
            return;
        }

        List<Account> accounts = new ArrayList<>();
        accountRepository.findAll().forEach(accounts::add);
        // TODO - Add active dates to account so it can be tracked for period.

        // Check that all the statements have a report.
        for(MonthStatus nextMonthStatus: getMonthStatusMap(accounts.size()).values()) {
            // Is this a complete month?
            if((nextMonthStatus.lockedStatementCount == nextMonthStatus.statementsFound) &&
                    (nextMonthStatus.lockedStatementCount == nextMonthStatus.activeAccounts) ){
                // All statements are locked, is there a report??
                if(!Files.exists(Paths.get(getMonthFilename(true, nextMonthStatus.year,nextMonthStatus.month)))) {
                    // Generate the month report.
                    generateReport(nextMonthStatus.year,nextMonthStatus.month);
                }

                if(nextMonthStatus.month == 12 && !Files.exists(Paths.get(getYearFilename(true,nextMonthStatus.year)))) {
                    // Generate the annual report.
                    generateAnnualReport(nextMonthStatus.year);
                }
            }
        }
    }

    public boolean reportsGeneratedForYear(int year) {
        // Check that the reports have been generated for the year specified.

        LOG.info("Checking - {}/{}", applicationProperties.getReportShare(), year);

        // Does the year directory exist?
        if(!Files.exists(Paths.get(applicationProperties.getReportShare() + "/" + year))) {
            return false;
        }

        LOG.info("Checking - {}", getYearFilename(true,year));

        if(!Files.exists(Paths.get(getYearFilename(true,year)))) {
            return false;
        }

        for(int month = 0; month < 12; month++) {
            LOG.info("Checking - {}", getMonthFilename(true,year, month + 1));

            if(!Files.exists(Paths.get(getMonthFilename(true, year, month + 1)))) {
                return false;
            }
        }

        return true;
    }
}
