package com.jbr.middletier.money.reporting;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.manager.LogoManager;
import com.jbr.middletier.money.xml.svg.CategorySvg;
import com.jbr.middletier.money.xml.svg.PieChartSvg;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
import org.apache.batik.transcoder.TranscoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);

    private final TransactionRepository transactionRepository;
    private final ResourceLoader resourceLoader;
    private final ApplicationProperties applicationProperties;
    private final LogoManager logoManager;
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                           ResourceLoader resourceLoader,
                           ApplicationProperties applicationProperties,
                           LogoManager logoManager,
                           StatementRepository statementRepository,
                           AccountRepository accountRepository ) {
        this.transactionRepository = transactionRepository;
        this.resourceLoader = resourceLoader;
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
        TranscoderOutput output_png_image = new TranscoderOutput(pngOstream);
        // Step-3: Create PNGTranscoder and define hints if required
        PNGTranscoder myConverter = new PNGTranscoder();
        myConverter.addTranscodingHint(PNGTranscoder.KEY_WIDTH,width);
        myConverter.addTranscodingHint(PNGTranscoder.KEY_HEIGHT,height);
        // Step-4: Convert and Write output
        myConverter.transcode(inputSvgImage, output_png_image);
        // Step 5- close / flush Output Stream
        pngOstream.flush();
        pngOstream.close();
    }

    private int getSplitIndex(String description) {
        // Split the description, have a maximum of 30 chars on the first line.

        // If less description 20 characters then leave as is.
        if(description.length() <= 20) {
            return 0;
        }

        // Is there a space that is after 15 and before 30 then use it as the split.
        String middleOfString = description.substring(15,Math.min(description.length(),30));
        if(middleOfString.contains(" ")) {
            return 15 + middleOfString.indexOf(" ");
        }

        // If we are still here, and the description is less than 30 then do not split.
        if(description.length() <= 30) {
            return 0;
        }

        // Split after 29 (negative so that a dash is added).
        return -29;
    }

    private void getRowForTransaction(StringBuilder table, Transaction nextTransaction) {
        if(nextTransaction == null) {
            table.append("<td/>");
            table.append("<td/>");
            table.append("<td/>");
            table.append("<td/>");
            table.append("<td/>");
            return;
        }

        DecimalFormat df = new DecimalFormat("#,###.00");

        // Date
        table.append("<td class=\"date\">");
        table.append(DateTimeFormatter.ofPattern("dd-MMM").format(nextTransaction.getDate())).append("<br/>");
        table.append(DateTimeFormatter.ofPattern("yyyy").format(nextTransaction.getDate()));
        table.append("</td>\n");

        // Account
        table.append("<td>");
        table.append("<img height=\"25px\" width=\"25px\" src=\"").append(applicationProperties.getReportWorking()).append(nextTransaction.getAccount().getId()).append(".png\"/>");
        table.append("</td>\n");

        // Category
        table.append("<td>");
        table.append("<img height=\"25px\" width=\"25px\" src=\"").append(applicationProperties.getReportWorking()).append(nextTransaction.getCategory().getId()).append(".png\"/>");
        table.append("</td>\n");

        // Description, split into 2 (if greater than 20 chars).
        String description1 = nextTransaction.getDescription();
        String description2 = "";
        int splitIndex = getSplitIndex(description1);
        boolean addDash = false;

        if(splitIndex < 0) {
            addDash = true;
            splitIndex *= -1;
        }

        // Situations
        if(splitIndex > 0) {
            description2 = description1.substring(splitIndex).trim();
            description1 = description1.substring(0,splitIndex);

            if(addDash) {
                description1 = description1 + "-";
            }
        }

        table.append("<td class=\"description\">");
        table.append(description1).append("<br/>");
        table.append(description2);
        table.append("</td>\n");

        // Amount
        if(nextTransaction.getAmount().getValue() < 0) {
            table.append("<td class=\"amount amount-debit\">");
        } else {
            table.append("<td class=\"amount\">");
        }
        table.append(df.format(nextTransaction.getAmount().getValue()));
        table.append("</td>\n");
    }

    private void outputTableHeader(StringBuilder table) {
        table.append("<table>\n");

        table.append("<tr>\n");
        table.append("<th>Date</th>\n");
        table.append("<th></th>\n");
        table.append("<th></th>\n");
        table.append("<th>Description</th>\n");
        table.append("<th>Amount</th>\n");
        table.append("<th/>");
        table.append("<th>Date</th>\n");
        table.append("<th></th>\n");
        table.append("<th></th>\n");
        table.append("<th>Description</th>\n");
        table.append("<th>Amount</th>\n");
        table.append("</tr>\n");
    }

    private String createTransactionTable(List<Transaction> transactions) {
        // Split into pages, first page can hold 19, then following pages can hold 36
        int pageRow = 0;


        int transactionSize = transactions.size();
        List<Transaction> column1 = new ArrayList<>(transactions.subList(0, (transactionSize) / 2));
        List<Transaction> column2 = new ArrayList<>(transactions.subList((transactionSize) / 2, transactionSize));

        StringBuilder table = new StringBuilder();

        table.append("<p style=\"page-break-after: always;\">&#xA0;</p>\n");
        outputTableHeader(table);

        for(int i = 0; i < Math.max(column1.size(), column2.size()); i++) {
            table.append("<tr>\n");
            getRowForTransaction(table,i < column1.size() ? column1.get(i) : null);
            table.append("<td class=\"center-column\"/>");
            getRowForTransaction(table,i < column2.size() ? column2.get(i) : null);
            table.append("</tr>\n");

            pageRow++;

            // Do we need a page break?
            if( pageRow == 35 ) {
                pageRow = 0;
                table.append("</table>\n");
                table.append("<p style=\"page-break-after: always;\">&#xA0;</p>\n");
                outputTableHeader(table);
            }
        }

        table.append("</table>\n");

        return table.toString();
    }

    private void createAccountImages(String workingDirectory, List<Transaction> transactions) throws IOException, TranscoderException {
        for(Transaction nextTransactions: transactions) {
            // Is there already a png for this account?
            String pngFilename = workingDirectory + "/" + nextTransactions.getAccount().getId() + ".png";
            File pngFile = new File(pngFilename);

            if(!pngFile.exists()) {
                // Create an SVG for the account.
                File svgFile = new File(workingDirectory + "/" + nextTransactions.getAccount().getId() + ".svg");
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
            String pngFilename = workingDirectory + "/" + nextTransactions.getCategory().getId() + ".png";
            File pngFile = new File(pngFilename);

            if(!pngFile.exists()) {
                // Create an SVG for the account.
                File svgFile = new File(workingDirectory + "/" + nextTransactions.getCategory().getId() + ".svg");
                try(PrintWriter svgWriter = new PrintWriter(svgFile)) {

                    ScalableVectorGraphics categorySvg = new CategorySvg(nextTransactions.getCategory());
                    svgWriter.write(categorySvg.getSvgAsString());
                    svgWriter.close();

                    // Create a PNG from SVG
                    createPngFromSvg(workingDirectory + "/" + nextTransactions.getCategory().getId() + ".svg",
                            workingDirectory + "/" + nextTransactions.getCategory().getId() + ".png",
                            100,
                            100);
                }
            }
        }
    }

    static class CategoryComparison implements Comparable<CategoryComparison> {
        public Category category;
        double thisMonth;
        double previousMonth;

        CategoryComparison(Category category) {
            this.category = category;
            this.thisMonth = 0;
            this.previousMonth = 0;
        }

        double getPercentageChange() {
            return ( ( this.thisMonth - this.previousMonth ) / this.previousMonth ) * 100.0;
        }

        @Override
        public int compareTo(CategoryComparison categoryComparison) {
            return Double.compare(this.thisMonth,categoryComparison.thisMonth);
        }
    }

    private String getAmountClass(double amount) {
        return amount >= 0 ? "amount" : "amount amount-debit";
    }

    private String createComparisonTable(List<Transaction> transactions, List<Transaction> previousTransactions, boolean year) {
        StringBuilder result = new StringBuilder();

        Map<String,CategoryComparison> comparisons = new HashMap<>();

        for(Transaction nextTransaction: transactions) {
            // Has this category already been seen?
            CategoryComparison categoryComparison;
            if(comparisons.containsKey(nextTransaction.getCategory().getId())) {
                categoryComparison = comparisons.get(nextTransaction.getCategory().getId());
            } else {
                categoryComparison = new CategoryComparison(nextTransaction.getCategory());
                comparisons.put(nextTransaction.getCategory().getId(),categoryComparison);
            }

            // Update the details on the category.
            categoryComparison.thisMonth += nextTransaction.getAmount().getValue();
        }

        for(Transaction nextTransaction: previousTransactions) {
            // Has this category already been seen?
            CategoryComparison categoryComparison;
            if(comparisons.containsKey(nextTransaction.getCategory().getId())) {
                categoryComparison = comparisons.get(nextTransaction.getCategory().getId());
            } else {
                categoryComparison = new CategoryComparison(nextTransaction.getCategory());
                comparisons.put(nextTransaction.getCategory().getId(),categoryComparison);
            }

            // Update the details on the category.
            categoryComparison.previousMonth += nextTransaction.getAmount().getValue();
        }

        result.append("<table>\n");
        result.append("<tr>\n");
        result.append("<th/>\n");
        result.append("<th/>\n");
        result.append("<th class=\"total-column\">Current Spend</th>\n");
        result.append("<th class=\"total-column\">Previous ").append(year ? "Year" : "Month").append("</th>\n");
        result.append("<th class=\"total-column\">Change in Spend</th>\n");
        result.append("</tr>\n");

        double totalSpending = 0.0;
        double previousTotalSpending = 0.0;
        DecimalFormat df = new DecimalFormat("#,##0.00");

        List<CategoryComparison> categories = new ArrayList<>(comparisons.values());
        Collections.sort(categories);

        for(CategoryComparison nextComparison: categories) {
            if(Boolean.FALSE.equals(nextComparison.category.getExpense())) {
                continue;
            }

            result.append("<tr>");
            LOG.info("-----------------------------------------------------------------------------");
            LOG.info(nextComparison.category.getId());
            LOG.info(Double.toString(nextComparison.thisMonth));
            LOG.info(Double.toString(nextComparison.previousMonth));
            LOG.info(Double.toString(nextComparison.getPercentageChange()));

            result.append("<td>\n");
            result.append("<img height=\"25px\" width=\"25px\" src=\"").append(applicationProperties.getReportWorking()).append(nextComparison.category.getId()).append(".png\"/>\n");
            result.append("</td>\n");
            result.append("<td>").append(nextComparison.category.getName()).append("</td>\n");
            result.append("<td class=\"").append(getAmountClass(nextComparison.thisMonth)).append("\">").append(df.format(nextComparison.thisMonth)).append("</td>\n");
            result.append("<td class=\"").append(getAmountClass(nextComparison.previousMonth)).append("\">").append(df.format(nextComparison.previousMonth)).append("</td>\n");

            if(nextComparison.previousMonth != 0.0 && nextComparison.getPercentageChange() != 0.0) {
                result.append("<td class=\"").append(getAmountClass(nextComparison.getPercentageChange())).append("\">").append(df.format(nextComparison.getPercentageChange())).append("%</td>\n");
            } else {
                result.append("<td/>\n");
            }

            result.append("</tr>\n");
            totalSpending += nextComparison.thisMonth;
            previousTotalSpending += nextComparison.previousMonth;
        }

        result.append("<tr>\n");
        result.append("<td/>\n");
        result.append("<td class=\"total-row\">Total</td>\n");
        result.append("<td class=\"total-row ").append(getAmountClass(totalSpending)).append("\">").append(df.format(totalSpending)).append("</td>\n");
        result.append("<td class=\"total-row ").append(getAmountClass(previousTotalSpending)).append("\">").append(df.format(previousTotalSpending)).append("</td>\n");

        double totalPercentageChange = 0.0;
        if (previousTotalSpending != 0.0) {
            totalPercentageChange = ((totalSpending - previousTotalSpending) / previousTotalSpending) * 100.0;
        }
        if(previousTotalSpending != 0.0 && totalPercentageChange != 0.0) {
            result.append("<td class=\"total-row ").append(getAmountClass(totalPercentageChange)).append("\">").append(df.format(totalPercentageChange)).append("%</td>\n");
        } else {
            result.append("<td/>\n");
        }
        result.append("</tr>\n");

        result.append("</table>\n");

        return result.toString();
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

    private String getTemplate(boolean year) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:html/report.html");
        InputStream is = resource.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);

        String template = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        StringBuilder body = new StringBuilder();

        if(year) {
            body.append("<!-- TITLE -->");
            body.append("<!-- PIE -->");
            body.append("<!-- TOTALS -->");

            for(int i = 0; i < 12; i++) {
                body.append("<p style=\"page-break-after: always;\">&#xA0;</p>\n");
                body.append("<!-- TITLE ").append(i).append(" -->");
                body.append("<!-- PIE ").append(i).append(" -->");
                body.append("<!-- TOTALS ").append(i).append(" -->");
            }
        } else {
            body.append("<!-- TITLE -->");
            body.append("<!-- PIE -->");
            body.append("<!-- TOTALS -->");
            body.append("<!-- TABLE -->");
        }

        return template.replace("<!--BODY-->", body.toString());
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

    private String addReportToTemplate(String template, String specific, int year, int month) throws IOException, TranscoderException {
        // Get all the transactions for the specified statement.
        List<Transaction> transactionList = transactionRepository.findByStatementIdYearAndStatementIdMonth(year,month);

        String titleMarker =  specific.length() > 0 ? "<!-- TITLE " + specific + " -->" : "<!-- TITLE -->";
        String pieMarker = specific.length() > 0 ? "<!-- PIE " + specific + " -->" : "<!-- PIE -->";
        String tableMarker = specific.length() > 0 ? "<!-- TABLE " + specific + " -->" : "<!-- TABLE -->";
        String totalsMarker = specific.length() > 0 ? "<!-- TOTALS " + specific + " -->" : "<!-- TOTALS -->";

        template = template.replace(titleMarker, "<h1>" + DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(year,month,1)) + "</h1>");

        LOG.info("Create pie chart.");
        createPieChart(transactionList,specific);
        template = template.replace(pieMarker, "<img class=\"pie\" height=\"400px\" width=\"400px\" src=\"" + applicationProperties.getReportWorking() + "/pie-" + specific + ".png" + "\"/>");

        LOG.info("Create table.");
        transactionList.sort(
                Comparator.comparing(Transaction::getDate)
        );
        template = template.replace(tableMarker, createTransactionTable(transactionList));

        // Get the previous month
        int previousMonth = month - 1;
        int previousYear = year;

        if(month == 1) {
            previousMonth = 12;
            previousYear--;
        }
        List<Transaction> previousTransactionList = transactionRepository.findByStatementIdYearAndStatementIdMonth(previousYear,previousMonth);
        template = template.replace(totalsMarker, createComparisonTable(transactionList,previousTransactionList,false));

        createImages(transactionList);
        createImages(previousTransactionList);

        return template;
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

        File htmlFile = new File(applicationProperties.getHtmlFilename());
        try(PrintWriter writer2 = new PrintWriter(htmlFile)) {

            String template = getTemplate(false);
            // TODO programatically create the HTML.
            template = addReportToTemplate(template, "", year, month);

            writer2.println(template);
        }

        generatePDF();

        // Copy the report to the share.
        copyFile(applicationProperties.getPDFFilename(),
                applicationProperties.getReportShare() + "/" + year,
                getMonthFilename(false,year,month));
    }

    public void generateAnnualReport(int year) throws IOException, TranscoderException, DocumentException {
        LOG.info("Generate annual report");

        // Get all the transactions for the specified statement.
        List<Transaction> transactionList = transactionRepository.findByStatementIdYear(year);

        createWorkingDirectories();

        File htmlFile = new File(applicationProperties.getHtmlFilename());
        List<Transaction> previousTransactionList;
        try(PrintWriter writer2 = new PrintWriter(htmlFile)) {

            // Get the file template.
            String template = getTemplate(true);

            template = template.replace("<!-- TITLE -->", "<h1>" + year + " Summary</h1>");

            LOG.info("Create pie chart.");
            createPieChart(transactionList, "yr");
            template = template.replace("<!-- PIE -->", "<img class=\"pie\" height=\"400px\" width=\"400px\" src=\"" + applicationProperties.getReportWorking() + "/pie-yr.png" + "\"/>");

            LOG.info("Insert totals");
            previousTransactionList = transactionRepository.findByStatementIdYear(year - 1);
            template = template.replace("<!-- TOTALS -->", createComparisonTable(transactionList, previousTransactionList, true));

            for (int i = 0; i < 12; i++) {
                template = addReportToTemplate(template, Integer.toString(i), year, i + 1);
            }

            writer2.println(template);
        }

        createImages(transactionList);
        createImages(previousTransactionList);

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

    @SuppressWarnings("RedundantCast")
    @Scheduled(cron = "#{@applicationProperties.reportSchedule}")
    public void regularReport() throws DocumentException, IOException, TranscoderException {
        // If this is enabled, then generate reports.
        if(!applicationProperties.getReportEnabled()) {
            return;
        }

        Iterable<Account> accounts = accountRepository.findAll();
        int activeAccounts = 0;
        // TODO - Add active dates to account so it can be tracked for period.
        for(Account ignored : accounts) activeAccounts++;

        Map<Long,MonthStatus> monthStatusMap = new HashMap<>();

        // Make sure reports have been generated where all statements are locked.
        Iterable<Statement> allStatements = statementRepository.findAll();
        for(Statement nextStatement: allStatements) {
            if(nextStatement.getLocked()) {
                // What is the ID?
                long statementId = (long)(nextStatement.getId().getYear() * 100 + nextStatement.getId().getMonth());
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

        // Check that all the statements have a report.
        for(MonthStatus nextMonthStatus: monthStatusMap.values()) {
            // Is this a complete month?
            if((nextMonthStatus.lockedStatementCount == nextMonthStatus.statementsFound) &&
                    (nextMonthStatus.lockedStatementCount == nextMonthStatus.activeAccounts) ){
                // All statements are locked, is there a report??
                if(!Files.exists(Paths.get(getMonthFilename(true, nextMonthStatus.year,nextMonthStatus.month)))) {
                    // Generate the month report.
                    generateReport(nextMonthStatus.year,nextMonthStatus.month);
                }

                if(nextMonthStatus.month == 12) {
                    if (!Files.exists(Paths.get(getYearFilename(true,nextMonthStatus.year)))) {
                        // Generate the annual report.
                        generateAnnualReport(nextMonthStatus.year);
                    }
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
