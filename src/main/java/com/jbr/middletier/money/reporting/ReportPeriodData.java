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
    private final String generatedAt;

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

        // Both totalIncome and totalSpending are positive values passed in
        BigDecimal net = totalIncome.subtract(totalSpending);
        this.netPositive = net.compareTo(BigDecimal.ZERO) >= 0;

        this.totalIncomeFormatted = formatAmount(totalIncome);
        this.totalSpendingFormatted = formatAmount(totalSpending);
        this.netFormatted = formatAmount(net.abs());
        this.totalCreditsFormatted = formatAmount(totalIncome);
        this.totalDebitsFormatted = formatAmount(totalSpending);
        this.generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm"));

        if (previousSpending.compareTo(BigDecimal.ZERO) == 0) {
            this.vsLastPeriodText = "N/A";
            this.vsLastPeriodCssClass = "kpi-value vs-na";
        } else {
            double pct = totalSpending.subtract(previousSpending)
                    .divide(previousSpending, 4, RoundingMode.HALF_UP)
                    .doubleValue() * 100.0;
            boolean up = pct > 0;
            this.vsLastPeriodText = String.format("%s %.0f%%", up ? "(+)" : "(-)", Math.abs(pct));
            this.vsLastPeriodCssClass = up ? "kpi-value vs-up" : "kpi-value vs-down";
        }
    }

    private static String formatAmount(BigDecimal amount) {
        return String.format("£%,.2f", amount.abs());
    }
}
