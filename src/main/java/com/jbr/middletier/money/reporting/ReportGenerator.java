package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.ReportStatus;
import com.jbr.middletier.money.data.primary.ReportStatusId;
import com.jbr.middletier.money.data.primary.repository.ReportStatusRepository;
import com.jbr.middletier.money.data.primary.Category;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.AccountRepository;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.dto.ReportDefinitionDTO;
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final EmailGenerator emailGenerator;
    private final ReportStatusRepository reportStatusRepository;

    @Autowired
    public ReportGenerator(TransactionRepository transactionRepository,
                           ApplicationProperties applicationProperties,
                           StatementRepository statementRepository,
                           AccountRepository accountRepository,
                           TemplateEngine templateEngine,
                           EmailGenerator emailGenerator,
                           ReportStatusRepository reportStatusRepository) {
        this.transactionRepository = transactionRepository;
        this.applicationProperties = applicationProperties;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.templateEngine = templateEngine;
        this.emailGenerator = emailGenerator;
        this.reportStatusRepository = reportStatusRepository;
    }

    // --- Report metrics and MD5 ---

    private static class ReportMetrics {
        final int totalTransactions;
        final int totalCategories;
        final BigDecimal sumCredits;
        final BigDecimal sumDebits;
        final String md5;

        ReportMetrics(List<Transaction> transactions) {
            this.totalTransactions = transactions.size();

            Set<String> categories = new HashSet<>();
            BigDecimal credits = BigDecimal.ZERO;
            BigDecimal debits = BigDecimal.ZERO;

            for (Transaction t : transactions) {
                BigDecimal amount = t.getAmount().getValue();
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    credits = credits.add(amount);
                } else {
                    debits = debits.add(amount.abs());
                }
                if (t.getCategory() != null) {
                    categories.add(t.getCategory().getId());
                }
            }

            this.totalCategories = categories.size();
            this.sumCredits = credits;
            this.sumDebits = debits;
            this.md5 = computeMd5(transactions);
        }

        private static String computeMd5(List<Transaction> transactions) {
            List<Transaction> sorted = new ArrayList<>(transactions);
            sorted.sort(Comparator.comparing(Transaction::getDate)
                    .thenComparing((Transaction t) -> t.getAmount().getValue())
                    .thenComparing((Transaction t) -> t.getAccount() != null ? t.getAccount().getId() : "")
                    .thenComparing((Transaction t) -> t.getCategory() != null ? t.getCategory().getId() : "")
                    .thenComparing((Transaction t) -> t.getDescription() != null ? t.getDescription() : ""));

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sorted.size(); i++) {
                Transaction t = sorted.get(i);
                if (i > 0) sb.append('\n');
                sb.append(t.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                sb.append('|');
                sb.append(t.getAmount().getValue().setScale(2, RoundingMode.HALF_UP).toPlainString());
                sb.append('|');
                sb.append(t.getAccount() != null ? t.getAccount().getId() : "");
                sb.append('|');
                sb.append(t.getCategory() != null ? t.getCategory().getId() : "");
                sb.append('|');
                sb.append(t.getDescription() != null ? t.getDescription() : "");
            }

            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder hex = new StringBuilder();
                for (byte b : hash) {
                    hex.append(String.format("%02x", b));
                }
                return hex.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MD5 not available", e);
            }
        }
    }

    private boolean metricsMatch(ReportStatus status, ReportMetrics metrics) {
        return status.getMd5Checksum().equals(metrics.md5)
                && status.getTotalTransactions() == metrics.totalTransactions
                && status.getTotalCategories() == metrics.totalCategories
                && status.getSumCredits().compareTo(metrics.sumCredits) == 0
                && status.getSumDebits().compareTo(metrics.sumDebits) == 0;
    }

    private void saveStatus(ReportStatusId id, ReportMetrics metrics, boolean successful) {
        ReportStatus status = new ReportStatus();
        status.setId(id);
        status.setTotalTransactions(metrics.totalTransactions);
        status.setTotalCategories(metrics.totalCategories);
        status.setSumCredits(metrics.sumCredits);
        status.setSumDebits(metrics.sumDebits);
        status.setMd5Checksum(metrics.md5);
        status.setLastChecked(applicationProperties.getToday());
        status.setSuccessful(successful);
        reportStatusRepository.save(status);
    }

    // --- Change-detection process methods ---

    private void processMonthlyReport(int year, int month) throws IOException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Transaction> transactions = transactionRepository.findByDateBetween(start, end);

        ReportMetrics metrics = new ReportMetrics(transactions);
        ReportStatusId id = new ReportStatusId(year, month, "monthly");

        Optional<ReportStatus> existing = reportStatusRepository.findById(id);
        if (existing.isPresent() && existing.get().isSuccessful() && metricsMatch(existing.get(), metrics)) {
            LOG.debug("Skipping monthly report {}/{} - data unchanged", year, month);
            return;
        }

        saveStatus(id, metrics, false);
        generateReportHtml(year, month);
        saveStatus(id, metrics, true);
        trySendReportEmail(new ReportDefinitionDTO("monthly", year, month));
    }

    private void processAnnualReport(int year) throws IOException {
        List<Transaction> transactions = transactionRepository.findByDateBetween(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));

        ReportMetrics metrics = new ReportMetrics(transactions);
        ReportStatusId id = new ReportStatusId(year, 0, "annual");

        Optional<ReportStatus> existing = reportStatusRepository.findById(id);
        if (existing.isPresent() && existing.get().isSuccessful() && metricsMatch(existing.get(), metrics)) {
            LOG.debug("Skipping annual report {} - data unchanged", year);
            return;
        }

        saveStatus(id, metrics, false);
        generateAnnualReportHtml(year);
        saveStatus(id, metrics, true);
        trySendReportEmail(new ReportDefinitionDTO("annual", year, null));
    }

    private void checkAnnualReports() throws IOException {
        for (int year : reportStatusRepository.findDistinctYearsWithMonthlyReports()) {
            if (reportStatusRepository.countSuccessfulMonthlyReports(year) == 12) {
                processAnnualReport(year);
            }
        }
    }

    // --- Public API methods (bypass window, apply change detection) ---

    public void generateReport(int year, int month) throws IOException {
        processMonthlyReport(year, month);
    }

    public void generateAnnualReport(int year) throws IOException {
        processAnnualReport(year);
    }

    // --- Scheduler ---

    @Scheduled(cron = "#{@applicationProperties.reportSchedule}")
    public void regularReport() throws IOException {
        if (!applicationProperties.getReportEnabled()) {
            return;
        }

        LocalDate today = applicationProperties.getToday();
        LocalDate endMonth = today.getDayOfMonth() > 15
                ? today.withDayOfMonth(1)
                : today.minusMonths(1).withDayOfMonth(1);
        LocalDate startMonth = endMonth.minusMonths(17); // 18 months inclusive

        LocalDate current = startMonth;
        while (!current.isAfter(endMonth)) {
            processMonthlyReport(current.getYear(), current.getMonthValue());
            current = current.plusMonths(1);
        }

        checkAnnualReports();
    }

    // --- HTML generation (private) ---

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void generateReportHtml(int year, int month) throws IOException {
        LOG.info("Generating monthly report {}/{}", year, month);

        createWorkingDirectories();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Transaction> transactions = transactionRepository.findByDateBetween(start, end);

        LocalDate prevStart = start.minusMonths(1);
        LocalDate prevEnd = prevStart.withDayOfMonth(prevStart.lengthOfMonth());
        List<Transaction> previousTransactionList = transactionRepository.findByDateBetween(prevStart, prevEnd);

        String title = DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(year, month, 1));
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
    private void generateAnnualReportHtml(int year) throws IOException {
        LOG.info("Generating annual report for {}", year);

        createWorkingDirectories();

        List<Transaction> transactions = transactionRepository.findByDateBetween(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        List<Transaction> previousTransactions = transactionRepository.findByDateBetween(
                LocalDate.of(year - 1, 1, 1), LocalDate.of(year - 1, 12, 31));

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

    private void trySendReportEmail(ReportDefinitionDTO definition) {
        try {
            emailGenerator.sendReport(definition);
        } catch (Exception e) {
            LOG.error("Failed to send report email for {}/{}: {}", definition.getYear(), definition.getMonth(), e.getMessage());
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
