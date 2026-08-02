package com.jbr.middletier.money.reporting;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
public class ReportPeriodData {
    private final String title;
    private final String subtitle;
    private final String totalIncomeFormatted;
    private final String totalSpendingFormatted;
    private final String netFormatted;
    private final boolean netPositive;
    private final String vsLastPeriodText;
    private final String vsLastPeriodCssClass;
    private final String totalCreditsFormatted;
    private final String totalDebitsFormatted;
    private final String donutSvg;
    private final String comparisonBarSvg;
    private final List<TransactionRow> transactions;
    private final String monthlyBarSvg;
    private final List<ReportPeriodData> monthSections;
    private final String generatedAt;

    // Constructor for monthly reports and for per-month sections within the annual report.
    public ReportPeriodData(String title,
                            String subtitle,
                            BigDecimal totalIncome,
                            BigDecimal totalSpending,
                            BigDecimal previousSpending,
                            String donutSvg,
                            String comparisonBarSvg,
                            List<TransactionRow> transactions) {
        this.title = title;
        this.subtitle = subtitle;
        this.donutSvg = donutSvg;
        this.comparisonBarSvg = comparisonBarSvg;
        this.transactions = transactions;
        this.monthlyBarSvg = null;
        this.monthSections = null;

        BigDecimal net = totalIncome.subtract(totalSpending);
        this.netPositive = net.compareTo(BigDecimal.ZERO) >= 0;
        this.totalIncomeFormatted = formatAmount(totalIncome);
        this.totalSpendingFormatted = formatAmount(totalSpending);
        this.netFormatted = formatAmount(net.abs());
        this.totalCreditsFormatted = formatAmount(totalIncome);
        this.totalDebitsFormatted = formatAmount(totalSpending);

        this.vsLastPeriodText = buildVsText(totalSpending, previousSpending);
        this.vsLastPeriodCssClass = buildVsClass(totalSpending, previousSpending);
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm"));
    }

    // Constructor for the annual summary — no transaction list; carries monthly bar chart and 12 month sub-sections.
    public ReportPeriodData(String title,
                            String subtitle,
                            BigDecimal totalIncome,
                            BigDecimal totalSpending,
                            BigDecimal previousSpending,
                            String donutSvg,
                            String comparisonBarSvg,
                            String monthlyBarSvg,
                            List<ReportPeriodData> monthSections) {
        this.title = title;
        this.subtitle = subtitle;
        this.donutSvg = donutSvg;
        this.comparisonBarSvg = comparisonBarSvg;
        this.monthlyBarSvg = monthlyBarSvg;
        this.monthSections = monthSections;
        this.transactions = null;

        BigDecimal net = totalIncome.subtract(totalSpending);
        this.netPositive = net.compareTo(BigDecimal.ZERO) >= 0;
        this.totalIncomeFormatted = formatAmount(totalIncome);
        this.totalSpendingFormatted = formatAmount(totalSpending);
        this.netFormatted = formatAmount(net.abs());
        this.totalCreditsFormatted = formatAmount(totalIncome);
        this.totalDebitsFormatted = formatAmount(totalSpending);

        this.vsLastPeriodText = buildVsText(totalSpending, previousSpending);
        this.vsLastPeriodCssClass = buildVsClass(totalSpending, previousSpending);
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm"));
    }

    private static String buildVsText(BigDecimal spending, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return "N/A";
        double pct = spending.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .doubleValue() * 100.0;
        return String.format("%s %.0f%%", pct > 0 ? "(+)" : "(-)", Math.abs(pct));
    }

    private static String buildVsClass(BigDecimal spending, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) return "kpi-value vs-na";
        double pct = spending.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .doubleValue();
        return pct > 0 ? "kpi-value vs-up" : "kpi-value vs-down";
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format("£%,.2f", amount.abs());
    }
}
