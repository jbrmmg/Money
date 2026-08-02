package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.xml.svg.ComparisonBarChartSvg;
import com.jbr.middletier.money.xml.svg.DonutChartSvg;
import com.jbr.middletier.money.xml.svg.MonthlyBarChartSvg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(ReportGenerator.class);

    private static final List<String> MONTH_NAMES = List.of(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );

    private static final String INDEX_CSS =
            "body{font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:40px auto;color:#333}" +
            "h1{color:#1a237e;border-bottom:2px solid #1a237e;padding-bottom:8px}" +
            "ul{list-style:none;padding:0}" +
            "li{margin:6px 0}" +
            "a{color:#1a237e;text-decoration:none;font-size:15px}" +
            "a:hover{text-decoration:underline}";

    private final TransactionRepository transactionRepository;
    private final ApplicationProperties applicationProperties;
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    private final TemplateEngine templateEngine;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                           ApplicationProperties applicationProperties,
                           StatementRepository statementRepository,
                           AccountRepository accountRepository,
                           TemplateEngine templateEngine) {
        this.transactionRepository = transactionRepository;
        this.applicationProperties = applicationProperties;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.templateEngine = templateEngine;
    }

    private Map<Category, BigDecimal> buildCategorySpendingMap(List<Transaction> transactions) {
        Map<Category, BigDecimal> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getCategory() == null || !Boolean.TRUE.equals(t.getCategory().getExpense())) {
                continue;
            }
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                map.merge(t.getCategory(), amount.abs(), BigDecimal::add);
            }
        }
        return map;
    }

    private ReportPeriodData buildReportData(String title, String subtitle,
                                              List<Transaction> transactions,
                                              List<Transaction> previousTransactions) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalSpending = BigDecimal.ZERO;
        BigDecimal previousSpending = BigDecimal.ZERO;

        List<TransactionRow> rows = new ArrayList<>();
        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                totalIncome = totalIncome.add(amount);
            } else {
                totalSpending = totalSpending.add(amount.abs());
            }
            boolean transfer = t.getOppositeTransactionId() != null;
            String categoryColour = t.getCategory() != null ? t.getCategory().getColour() : null;
            String categoryName  = t.getCategory() != null ? t.getCategory().getName()   : "Uncategorised";
            String accountName   = t.getAccount()  != null ? t.getAccount().getName()    : "";
            rows.add(new TransactionRow(t.getDate(), accountName, categoryColour,
                    categoryName, t.getDescription(), amount, transfer));
        }

        for (Transaction t : previousTransactions) {
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                previousSpending = previousSpending.add(amount.abs());
            }
        }

        rows.sort(Comparator.comparing(TransactionRow::getDate));

        Map<Category, BigDecimal> currentCategorySpending = buildCategorySpendingMap(transactions);
        Map<Category, BigDecimal> previousCategorySpending = buildCategorySpendingMap(previousTransactions);

        String donutSvg = new DonutChartSvg(transactions, totalSpending).getInlineSvgString();
        String comparisonBarSvg = new ComparisonBarChartSvg(currentCategorySpending, previousCategorySpending).getInlineSvgString();

        return new ReportPeriodData(title, subtitle, totalIncome, totalSpending,
                previousSpending, donutSvg, comparisonBarSvg, rows);
    }

    private ReportPeriodData buildSectionData(String title, List<Transaction> transactions,
                                               List<Transaction> previousTransactions) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalSpending = BigDecimal.ZERO;
        BigDecimal previousSpending = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) > 0) totalIncome = totalIncome.add(amount);
            else totalSpending = totalSpending.add(amount.abs());
        }
        for (Transaction t : previousTransactions) {
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) < 0) previousSpending = previousSpending.add(amount.abs());
        }

        Map<Category, BigDecimal> currentCat = buildCategorySpendingMap(transactions);
        Map<Category, BigDecimal> previousCat = buildCategorySpendingMap(previousTransactions);

        String donutSvg = new DonutChartSvg(transactions, totalSpending).getInlineSvgString();
        String comparisonBarSvg = new ComparisonBarChartSvg(currentCat, previousCat).getInlineSvgString();

        return new ReportPeriodData(title, "Monthly Summary", totalIncome, totalSpending,
                previousSpending, donutSvg, comparisonBarSvg, (List<TransactionRow>) null);
    }

    private String buildHtml(ReportPeriodData data) {
        Context ctx = new Context();
        ctx.setVariable("data", data);
        return templateEngine.process("report/monthly", ctx);
    }

    private String buildAnnualHtml(ReportPeriodData data) {
        Context ctx = new Context();
        ctx.setVariable("data", data);
        return templateEngine.process("report/annual", ctx);
    }

    private void writeHtmlDebug(String html) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(applicationProperties.getHtmlFilename())),
                StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        LOG.info("Debug HTML written to {}", applicationProperties.getHtmlFilename());
    }

    private void createWorkingDirectories() {
        if (!Files.exists(Paths.get(applicationProperties.getReportWorking()))) {
            //noinspection ResultOfMethodCallIgnored
            new File(applicationProperties.getReportWorking()).mkdirs();
        }
        if (!Files.exists(Paths.get(applicationProperties.getReportShare()))) {
            //noinspection ResultOfMethodCallIgnored
            new File(applicationProperties.getReportShare()).mkdirs();
        }
    }

    private void generateYearIndex(int year) throws IOException {
        String yearDir = applicationProperties.getReportShare() + "/" + year;

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\"/>\n");
        sb.append("  <title>").append(year).append(" Reports</title>\n");
        sb.append("  <style>").append(INDEX_CSS).append("</style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("<h1>").append(year).append(" Reports</h1>\n<ul>\n");

        if (Files.exists(Paths.get(yearDir + "/annual.html"))) {
            sb.append("  <li><a href=\"annual.html\">Annual Report &#8211; ").append(year).append("</a></li>\n");
        }
        for (int m = 1; m <= 12; m++) {
            String monthFile = MONTH_NAMES.get(m - 1) + ".html";
            if (Files.exists(Paths.get(yearDir + "/" + monthFile))) {
                LocalDate d = LocalDate.of(year, m, 1);
                String label = DateTimeFormatter.ofPattern("MMMM yyyy").format(d);
                sb.append("  <li><a href=\"").append(monthFile).append("\">").append(label).append("</a></li>\n");
            }
        }

        sb.append("</ul>\n</body>\n</html>\n");

        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(yearDir + "/index.html")), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
        LOG.info("Written year index to {}/index.html", yearDir);
    }

    private void generateRootIndex() throws IOException {
        String shareDir = applicationProperties.getReportShare();

        List<Integer> years = new ArrayList<>();
        File[] dirs = new File(shareDir).listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                try {
                    int year = Integer.parseInt(dir.getName());
                    if (Files.exists(Paths.get(dir.getPath() + "/index.html"))) {
                        years.add(year);
                    }
                } catch (NumberFormatException ignored) {
                    // not a year directory
                }
            }
        }
        years.sort(Collections.reverseOrder());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\"/>\n");
        sb.append("  <title>Financial Reports</title>\n");
        sb.append("  <style>").append(INDEX_CSS).append("</style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("<h1>Financial Reports</h1>\n<ul>\n");
        for (int year : years) {
            sb.append("  <li><a href=\"").append(year).append("/index.html\">").append(year).append("</a></li>\n");
        }
        sb.append("</ul>\n</body>\n</html>\n");

        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(shareDir + "/index.html")), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
        LOG.info("Written root index to {}/index.html", shareDir);
    }

    private String getYearFilename(long year) {
        return applicationProperties.getReportShare() + "/" + year + "/annual.html";
    }

    private String getMonthFilename(int year, int month) {
        return applicationProperties.getReportShare() + "/" + year + "/" + MONTH_NAMES.get(month - 1) + ".html";
    }

    public Map<Integer, MonthStatus> getMonthStatusMap(int activeAccounts) {
        Map<Integer, MonthStatus> monthStatusMap = new HashMap<>();

        Iterable<Statement> allStatements = statementRepository.findAll();
        for (Statement nextStatement : allStatements) {
            if (nextStatement.getLocked()) {
                Integer statementId = (nextStatement.getId().getYear() * 100 + nextStatement.getId().getMonth());
                MonthStatus nextMonthStatus;

                if (monthStatusMap.containsKey(statementId)) {
                    nextMonthStatus = monthStatusMap.get(statementId);
                } else {
                    nextMonthStatus = new MonthStatus();
                    nextMonthStatus.month = nextStatement.getId().getMonth();
                    nextMonthStatus.year = nextStatement.getId().getYear();
                    nextMonthStatus.lockedStatementCount = 0;
                    nextMonthStatus.statementsFound = 0;
                    nextMonthStatus.activeAccounts = activeAccounts;
                    monthStatusMap.put(statementId, nextMonthStatus);
                }

                nextMonthStatus.lockedStatementCount++;
                nextMonthStatus.statementsFound++;
            }
        }

        return monthStatusMap;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void generateReport(int year, int month) throws IOException {
        LOG.info("Generate report");

        createWorkingDirectories();
        List<Transaction> transactions = transactionRepository.findByStatementIdYearAndStatementIdMonth(year, month);

        int previousMonth = month - 1;
        int previousYear = year;
        if (month == 1) {
            previousMonth = 12;
            previousYear--;
        }
        List<Transaction> previousTransactionList = transactionRepository.findByStatementIdYearAndStatementIdMonth(previousYear, previousMonth);

        LocalDate reportDate = LocalDate.of(year, month, 1);
        String title = DateTimeFormatter.ofPattern("MMMM yyyy").format(reportDate);

        ReportPeriodData data = buildReportData(title, "Monthly Financial Report", transactions, previousTransactionList);
        String html = buildHtml(data);

        if (applicationProperties.isReportDebugHtml()) {
            writeHtmlDebug(html);
        }

        String yearDir = applicationProperties.getReportShare() + "/" + year;
        if (!Files.exists(Paths.get(yearDir))) {
            new File(yearDir).mkdirs();
        }
        String htmlPath = getMonthFilename(year, month);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(htmlPath)), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        LOG.info("Written report HTML to {}", htmlPath);

        generateYearIndex(year);
        generateRootIndex();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void generateAnnualReport(int year) throws IOException {
        LOG.info("Generate annual report for {}", year);

        createWorkingDirectories();

        List<Transaction> transactions = transactionRepository.findByStatementIdYear(year);
        List<Transaction> previousTransactions = transactionRepository.findByStatementIdYear(year - 1);

        // Group by month for efficient per-section lookup.
        Map<Integer, List<Transaction>> byMonth = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().getMonthValue()));
        Map<Integer, List<Transaction>> prevByMonth = previousTransactions.stream()
                .collect(Collectors.groupingBy(t -> t.getDate().getMonthValue()));

        List<MonthlyBarChartSvg.MonthData> barData = new ArrayList<>();
        List<ReportPeriodData> monthSections = new ArrayList<>();
        BigDecimal annualIncome = BigDecimal.ZERO;
        BigDecimal annualSpending = BigDecimal.ZERO;

        for (int month = 1; month <= 12; month++) {
            List<Transaction> monthTrans = byMonth.getOrDefault(month, Collections.emptyList());
            List<Transaction> prevMonthTrans = (month == 1)
                    ? prevByMonth.getOrDefault(12, Collections.emptyList())
                    : byMonth.getOrDefault(month - 1, Collections.emptyList());

            BigDecimal monthIncome = BigDecimal.ZERO;
            BigDecimal monthSpending = BigDecimal.ZERO;
            for (Transaction t : monthTrans) {
                BigDecimal amount = t.getAmount().getValue();
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    monthIncome = monthIncome.add(amount);
                    annualIncome = annualIncome.add(amount);
                } else {
                    monthSpending = monthSpending.add(amount.abs());
                    annualSpending = annualSpending.add(amount.abs());
                }
            }
            barData.add(new MonthlyBarChartSvg.MonthData(monthIncome, monthSpending));

            String sectionTitle = DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(year, month, 1));
            monthSections.add(buildSectionData(sectionTitle, monthTrans, prevMonthTrans));
        }

        BigDecimal prevAnnualSpending = BigDecimal.ZERO;
        for (Transaction t : previousTransactions) {
            BigDecimal amount = t.getAmount().getValue();
            if (amount.compareTo(BigDecimal.ZERO) < 0) prevAnnualSpending = prevAnnualSpending.add(amount.abs());
        }

        String monthlyBarSvg = new MonthlyBarChartSvg(barData).getInlineSvgString();
        Map<Category, BigDecimal> currentCat = buildCategorySpendingMap(transactions);
        Map<Category, BigDecimal> previousCat = buildCategorySpendingMap(previousTransactions);
        String donutSvg = new DonutChartSvg(transactions, annualSpending).getInlineSvgString();
        String comparisonBarSvg = new ComparisonBarChartSvg(currentCat, previousCat).getInlineSvgString();

        ReportPeriodData annualData = new ReportPeriodData(
                year + " Annual Report", "Annual Financial Report",
                annualIncome, annualSpending, prevAnnualSpending,
                donutSvg, comparisonBarSvg,
                monthlyBarSvg, monthSections);

        String html = buildAnnualHtml(annualData);

        if (applicationProperties.isReportDebugHtml()) {
            writeHtmlDebug(html);
        }

        String yearDir = applicationProperties.getReportShare() + "/" + year;
        if (!Files.exists(Paths.get(yearDir))) {
            new File(yearDir).mkdirs();
        }
        String annualPath = getYearFilename(year);
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(annualPath)), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        LOG.info("Annual report written to {}", annualPath);

        generateYearIndex(year);
        generateRootIndex();
    }

    @Scheduled(cron = "#{@applicationProperties.reportSchedule}")
    public void regularReport() throws IOException {
        if (!applicationProperties.getReportEnabled()) {
            return;
        }

        List<Account> accounts = new ArrayList<>();
        accountRepository.findAll().forEach(a -> {
            if (Boolean.FALSE.equals(a.getClosed())) {
                accounts.add(a);
            }
        });

        for (MonthStatus nextMonthStatus : getMonthStatusMap(accounts.size()).values()) {
            if ((nextMonthStatus.lockedStatementCount == nextMonthStatus.statementsFound) &&
                    (nextMonthStatus.lockedStatementCount == nextMonthStatus.activeAccounts)) {
                if (!Files.exists(Paths.get(getMonthFilename(nextMonthStatus.year, nextMonthStatus.month)))) {
                    generateReport(nextMonthStatus.year, nextMonthStatus.month);
                }
                if (nextMonthStatus.month == 12 && !Files.exists(Paths.get(getYearFilename(nextMonthStatus.year)))) {
                    generateAnnualReport(nextMonthStatus.year);
                }
            }
        }
    }

    public boolean reportsGeneratedForYear(int year) {
        LOG.info("Checking - {}/{}", applicationProperties.getReportShare(), year);

        if (!Files.exists(Paths.get(applicationProperties.getReportShare() + "/" + year))) {
            return false;
        }

        String yearFilename = getYearFilename(year);
        LOG.info("Checking - {}", yearFilename);
        if (!Files.exists(Paths.get(yearFilename))) {
            return false;
        }

        for (int month = 0; month < 12; month++) {
            String monthFilename = getMonthFilename(year, month + 1);
            LOG.info("Checking - {}", monthFilename);
            if (!Files.exists(Paths.get(monthFilename))) {
                return false;
            }
        }

        return true;
    }
}
