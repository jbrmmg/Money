package com.jbr.middletier.money.xml.html;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.util.CategoryComparison;
import com.jbr.middletier.money.util.FinancialAmount;
import org.apache.commons.text.WordUtils;
import org.jdom2.Element;
import org.jdom2.Text;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportHtml extends HyperTextMarkupLanguage {
    public enum ReportType { ANNUAL, MONTH }

    private final List<Transaction> transactions;
    private final List<Transaction> previousTransactions;
    private final LocalDate reportDate;
    private final String workingDirectory;
    private final ReportType type;

    private static final String HTML_TD_DATE = "date";
    private static final String HTML_TD_DESCRIPTION = "description";
    private static final String HTML_TD_AMOUNT = "amount";
    private static final String HTML_TD_AMOUNT_DEBIT = "amount-debit";
    private static final String HTML_TD_CENTER = "center-column";
    private static final String HTML_TD_TOTAL = "total-column";
    private static final String HTML_TD_ROW = "total-row";
    private static final String HTML_FONT_ARIAL = "Arial";
    private static final String HTML_FONT_HELVETICA = "Helvetica";
    private static final String HTML_PIE = "pie";

    public ReportHtml(List<Transaction> transactions, List<Transaction> previousTransactions, LocalDate reportDate, String workingDirectory, ReportType type) {
        super(Map.of(HTML_NO_BREAK_SPACE_ESC, HTML_NO_BREAK_SPACE, HTML_BR_ESC, HTML_BR));
        this.transactions = transactions;
        this.transactions.sort(Comparator.comparing(Transaction::getDate));
        this.previousTransactions = previousTransactions;
        this.previousTransactions.sort(Comparator.comparing(Transaction::getDate));
        this.reportDate = reportDate;
        this.workingDirectory = workingDirectory;
        this.type = type;
    }

    private CascadingStyleSheet generateCSS() {
        CascadingStyleSheet result = new CascadingStyleSheet();

        result.addRule(getCssRule("@page", Map.of(HTML_CSS_MARGIN, formatedUnit(UnitType.PT,10))));
        result.addRule(getCssRule(HTML_BODY, Map.of(HTML_CSS_FONT_FAMILY, fontString(HTML_FONT_ARIAL,HTML_FONT_HELVETICA,HTML_CSS_FONT_SAN_SERIF), HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,12))));
        result.addRule(getCssRule(HTML_STYLE_H1, Map.of(HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,24), HTML_CSS_FONT_WEIGHT, HTML_CSS_BOLDER)));
        result.addRule(getCssRule(HTML_STYLE_H2, Map.of(HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,14), HTML_CSS_FONT_WEIGHT, HTML_CSS_BOLD)));
        result.addRule(getCssRule(HTML_TABLE, Map.of(HTML_BORDER_SPACING, formatedUnit(UnitType.PX,0))));
        result.addRule(getCssRule(HTML_TD, Map.of(HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,10), HTML_WHITESPACE, HTML_NOWRAP)));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_DATE), Map.of(HTML_CSS_TEXT_ALIGN, HTML_CSS_RIGHT)));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_DESCRIPTION), Map.of(HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,8), HTML_WHITESPACE, HTML_NOWRAP)));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_AMOUNT), Map.of(HTML_CSS_COLOUR,"#000000", HTML_CSS_TEXT_ALIGN, HTML_CSS_RIGHT)));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_AMOUNT_DEBIT), Map.of(HTML_CSS_COLOUR,"#FF0000")));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_CENTER), Map.of(HTML_BORDER_RIGHT, borderString("darkblue"), HTML_CSS_WIDTH,  formatedUnit(UnitType.PX,10))));
        result.addRule(getCssRule(selector(HTML_TD,HTML_TD_ROW), Map.of(HTML_BORDER_TOP, borderString("black"), HTML_PADDING_TOP, formatedUnit(UnitType.PX,4), HTML_CSS_FONT_SIZE, formatedUnit(UnitType.PX,14), HTML_CSS_FONT_WEIGHT, HTML_CSS_BOLD)));
        result.addRule(getCssRule(selector(HTML_TH,HTML_TD_TOTAL), Map.of(HTML_PADDING_LEFT, formatedUnit(UnitType.PX,30))));
        result.addRule(getCssRule("img.pie", Map.of(HTML_DISPLAY, HTML_BLOCK, HTML_CSS_MARGIN_RIGHT, HTML_CSS_MARGIN_AUTO, HTML_CSS_MARGIN_LEFT, HTML_CSS_MARGIN_AUTO)));

        return result;
    }

    private String getStyleSheet() {
        CSSWriterSettings settings = new CSSWriterSettings(ECSSVersion.CSS30, false);
        settings.setRemoveUnnecessaryCode(true);
        CSSWriter cssWriter = new CSSWriter(settings);

        return cssWriter.getCSSAsString(generateCSS());
    }

    @Override
    protected Element getHeader() {
        Element title = new Element(HTML_TITLE)
                .setContent(new Text("Report"));

        Element style = new Element(HTML_STYLE)
                .setContent(new Text(getStyleSheet()));

        return new Element(HTML_HEAD)
                .addContent(title)
                .addContent(style);
    }

    private static void addDateToRow(Element row, LocalDate transactionDate) {
        Element date = new Element(HTML_TD);
        date.setAttribute(HTML_CSS_CLASS,HTML_TD_DATE);
        date.setText(DateTimeFormatter.ofPattern("dd-MMM").format(transactionDate) +
                HTML_BR +
                DateTimeFormatter.ofPattern("yyyy").format(transactionDate));
        row.addContent(date);
    }

    private static void addImageToRow(Element row, String imagePath, String imageName) {
        Element column = new Element(HTML_TD);
        Element image = new Element(HTML_IMG);
        image.setAttribute(HTML_CSS_HEIGHT,formatedUnit(UnitType.PX,25));
        image.setAttribute(HTML_CSS_WIDTH,formatedUnit(UnitType.PX,25));
        image.setAttribute(HTML_SRC_ATTRIBUTE, imagePath + imageName + ".png");
        column.addContent(image);
        row.addContent(column);
    }

    private static void addDescriptionToRow(Element row, String description) {
        // Split the string into lines, break at words.
        String[] lines = WordUtils.wrap(description,30,"\n",true," ").split("\n");

        // Split that into separate string.
        Element descriptionElement = new Element(HTML_TD)
                .setAttribute(HTML_CSS_CLASS,HTML_TD_DESCRIPTION);

        if(lines.length <= 1) {
            descriptionElement.setText(lines[0]);
        } else {
            descriptionElement.setText(lines[0] + HTML_BR + lines[1]);
        }

        row.addContent(descriptionElement);
    }

    private static void addAmountToRow(Element row, FinancialAmount amount, String positiveClass, String negativeClass) {
        row.addContent(new Element(HTML_TD)
                .setAttribute(HTML_CSS_CLASS, amount.getValue() < 0 ? negativeClass : positiveClass)
                .setText(amount.toString()));
    }

    private static void addTransactionToRow(Element row, Transaction transaction, String imagePath) {
        if(transaction == null) {
            // Add blank columns
            for(int i = 0; i < 5; i++) {
                row.addContent(new Element(HTML_TD));
            }
            return;
        }

        // Date
        addDateToRow(row,transaction.getDate());

        // Account
        addImageToRow(row,imagePath,transaction.getAccount().getId());

        // Category
        addImageToRow(row,imagePath,transaction.getCategory().getId());

        // Description
        addDescriptionToRow(row, Optional.of(transaction.getDescription()).orElse(""));

        // Amount
        addAmountToRow(row,transaction.getAmount(), HTML_TD_AMOUNT, concatenateClass(HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT));
    }

    private static class TransactionPair {
        private Transaction left;
        private Transaction right;

        public TransactionPair() {
            this.left = null;
            this.right = null;
        }

        public Transaction getLeft() {
            return left;
        }

        public void setLeft(Transaction left) {
            this.left = left;
        }

        public Transaction getRight() {
            return right;
        }

        public void setRight(Transaction right) {
            this.right = right;
        }
    }

    private Element getTransactionsTable() {
        Element result = new Element(HTML_TABLE);

        // Add the title
        result.addContent(new Element(HTML_TR)
                .addContent(new Element(HTML_TH).setText("Date"))
                .addContent(new Element(HTML_TH).setText(""))
                .addContent(new Element(HTML_TH).setText(""))
                .addContent(new Element(HTML_TH).setText("Description"))
                .addContent(new Element(HTML_TH).setText("Amount"))
                .addContent(new Element(HTML_TH).setText(""))
                .addContent(new Element(HTML_TH).setText("Date"))
                .addContent(new Element(HTML_TH).setText(""))
                .addContent(new Element(HTML_TH).setText(""))
                .addContent(new Element(HTML_TH).setText("Description"))
                .addContent(new Element(HTML_TH).setText("Amount")));

        // Divide the transactions into columns.
        List<TransactionPair> transactionPairs = new ArrayList<>();
        int transactionCount = 0;
        for(Transaction nextTransaction : this.transactions) {
            if(transactionPairs.size() >= ((this.transactions.size() + 1) / 2)) {
                TransactionPair newPair = transactionPairs.get(transactionCount - ((this.transactions.size() + 1) / 2));
                newPair.setRight(nextTransaction);
            } else {
                TransactionPair newPair = new TransactionPair();
                newPair.setLeft(nextTransaction);
                transactionPairs.add(newPair);
            }
            transactionCount++;
        }

        // Create rows for each pair.
        for(TransactionPair nextPair : transactionPairs) {
            Element row = new Element(HTML_TR);

            addTransactionToRow(row,nextPair.getLeft(),this.workingDirectory);

            Element centerColumn = new Element(HTML_TD);
            centerColumn.setAttribute(HTML_CSS_CLASS, HTML_TD_CENTER);
            row.addContent(centerColumn);

            addTransactionToRow(row,nextPair.getRight(),this.workingDirectory);

            result.addContent(row);
        }

        return result;
    }

    private Element getComparisonRow(String categoryId, CategoryComparison categoryComparison) {
        Element comparisonRow = new Element(HTML_TR);

        // Image and name
        addImageToRow(comparisonRow,workingDirectory,categoryId);
        comparisonRow.addContent(new Element(HTML_TD).setText(categoryComparison.getCategory().getName()));

        // Amounts
        addAmountToRow(comparisonRow,categoryComparison.getThisMonth(), HTML_TD_AMOUNT, concatenateClass(HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT));
        addAmountToRow(comparisonRow,categoryComparison.getPreviousMonth(), HTML_TD_AMOUNT, concatenateClass(HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT));

        // Percentage change, if anything.
        if(categoryComparison.getPreviousMonth().getValue() != 0.0 && categoryComparison.getPercentageChange() != 0.0) {
            comparisonRow.addContent(new Element(HTML_TD)
                    .setAttribute(HTML_CSS_CLASS, categoryComparison.getPercentageChange() < 0 ? concatenateClass(HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT) : HTML_TD_AMOUNT)
                    .setText(new DecimalFormat("#").format(categoryComparison.getPercentageChange()) + "%"));
        } else {
            comparisonRow.addContent(new Element(HTML_TD));
        }

        return comparisonRow;
    }

    private String getPreviousTitle(boolean month) {
        if(month) {
            return "Previous Month";
        }

        return "Previous Year";
    }

    private Element getComparisonTable(List<Transaction> transactions, List<Transaction> previousTransactions,  boolean month) {
        Element result = new Element(HTML_TABLE);

        // Add the header.
        result.addContent(new Element(HTML_TR)
                .addContent(new Element(HTML_TH))
                .addContent(new Element(HTML_TH))
                .addContent(new Element(HTML_TH).setAttribute(HTML_CSS_CLASS,HTML_TD_TOTAL).setText("Current Spend"))
                .addContent(new Element(HTML_TH).setAttribute(HTML_CSS_CLASS,HTML_TD_TOTAL).setText(getPreviousTitle(month)))
                .addContent(new Element(HTML_TH).setAttribute(HTML_CSS_CLASS,HTML_TD_TOTAL).setText("Change in Spend")));

        Map<String,CategoryComparison> comparisons = CategoryComparison.categoryCompare(transactions,previousTransactions);

        // Add the categories
        FinancialAmount totalThis = new FinancialAmount();
        FinancialAmount totalPrevious = new FinancialAmount();
        for(Map.Entry<String,CategoryComparison> next : comparisons.entrySet()) {
            result.addContent(getComparisonRow(next.getKey(),next.getValue()));
            totalThis.increment(next.getValue().getThisMonth());
            totalPrevious.increment(next.getValue().getPreviousMonth());
        }

        // Add the totals
        Element totalRow = new Element(HTML_TR);
        result.addContent(totalRow);

        totalRow
            .addContent(new Element(HTML_TD))
            .addContent(new Element(HTML_TD)
                .setAttribute(HTML_CSS_CLASS,HTML_TD_ROW)
                .setText("Total"));
        addAmountToRow(totalRow, totalThis, concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT), concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT));
        addAmountToRow(totalRow, totalPrevious, concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT), concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT));

        double totalPercentage = 0.0;
        if(totalPrevious.getValue() != 0.0) {
            totalPercentage = ((totalThis.getValue() - totalPrevious.getValue()) / totalPrevious.getValue()) * 100.0;
        }

        // Add the percentage total.
        if(totalPrevious.getValue() != 0.0 && totalPercentage != 0.0) {
            totalRow.addContent(new Element(HTML_TD)
                    .setAttribute(HTML_CSS_CLASS, totalPercentage < 0 ? concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT,HTML_TD_AMOUNT_DEBIT) : concatenateClass(HTML_TD_ROW,HTML_TD_AMOUNT))
                    .setText(new DecimalFormat("#").format(totalPercentage) + "%"));
        } else {
            totalRow.addContent(new Element(HTML_TD));
        }

        return result;
    }

    private Element getMonthReportBody() {
        Element titleText = new Element(HTML_STYLE_H1)
                .addContent(DateTimeFormatter.ofPattern("MMMM yyyy").format(this.reportDate));

        Element pie = new Element(HTML_IMG)
                .setAttribute(HTML_CSS_CLASS, HTML_PIE)
                .setAttribute(HTML_CSS_HEIGHT, formatedUnit(UnitType.PX,400))
                .setAttribute(HTML_CSS_WIDTH, formatedUnit(UnitType.PX,400))
                .setAttribute(HTML_SRC_ATTRIBUTE, this.workingDirectory + "/pie-.png");

        Element pageBreak = new Element(HTML_P)
                .setAttribute(HTML_STYLE, "page-break-after: always;")
                .setText(HTML_NO_BREAK_SPACE);

        return new Element(HTML_BODY)
                .addContent(titleText)
                .addContent(pie)
                .addContent(getComparisonTable(this.transactions, this.previousTransactions, true))
                .addContent(pageBreak)
                .addContent(getTransactionsTable());
    }

    private List<Transaction> filterTransactions(List<Transaction> transactions, int month) {
        return transactions
                .stream()
                .filter(t -> t.getStatement().getId().getMonth() == month)
                .collect(Collectors.toList());
    }

    private Element getAnnualReportBody() {
        Element titleText = new Element(HTML_STYLE_H1)
                .addContent(DateTimeFormatter.ofPattern("yyyy").format(this.reportDate) + " Summary");

        Element pie = new Element(HTML_IMG)
                .setAttribute(HTML_CSS_CLASS, HTML_PIE)
                .setAttribute(HTML_CSS_HEIGHT, formatedUnit(UnitType.PX,400))
                .setAttribute(HTML_CSS_WIDTH, formatedUnit(UnitType.PX,400))
                .setAttribute(HTML_SRC_ATTRIBUTE, this.workingDirectory + "/pie-yr.png");

        Element body = new Element(HTML_BODY)
                .addContent(titleText)
                .addContent(pie)
                .addContent(getComparisonTable(this.transactions, this.previousTransactions,false));

        for(int i = 0; i < 12; i++) {
            body.addContent(new Element(HTML_P)
                    .setAttribute(HTML_STYLE, "page-break-after: always;")
                    .setText(HTML_NO_BREAK_SPACE));
            body.addContent(new Element(HTML_STYLE_H1)
                    .addContent(DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(this.reportDate.getYear(),i + 1,1))));
            body.addContent(new Element(HTML_IMG)
                    .setAttribute(HTML_CSS_CLASS, HTML_PIE)
                    .setAttribute(HTML_CSS_HEIGHT, formatedUnit(UnitType.PX,400))
                    .setAttribute(HTML_CSS_WIDTH, formatedUnit(UnitType.PX,400))
                    .setAttribute(HTML_SRC_ATTRIBUTE, this.workingDirectory + "/pie-" + i + ".png"));

            List<Transaction> currentMonth = filterTransactions(this.transactions, i+1);
            List<Transaction> previousMonth;
            if(i == 0) {
                previousMonth =filterTransactions(this.previousTransactions, 12);
            } else {
                previousMonth =filterTransactions(this.transactions, i);
            }
            body.addContent(getComparisonTable(currentMonth, previousMonth,true));
        }

        return body;
    }

    @Override
    protected Element getBody() {
        if(this.type == ReportType.MONTH) {
            return getMonthReportBody();
        }

        return getAnnualReportBody();
    }
}
