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
    private static final String BODY = "body";

    public enum ReportType { ANNUAL, MONTH }

    private final List<Transaction> transactions;
    private final List<Transaction> previousTransactions;
    private final LocalDate reportDate;
    private final String workingDirectory;
    private final ReportType type;

    public ReportHtml(List<Transaction> transactions, List<Transaction> previousTransactions, LocalDate reportDate, String workingDirectory, ReportType type) {
        super(Map.of("&amp;#xA0;","&#xA0;","&lt;br/&gt;","<br/>"));
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

        result.addRule(getCssRule("@page", Map.of("margin", "10pt")));
        result.addRule(getCssRule("body", Map.of("font-family","Arial, Helvetica, sans-serif", "font-size", "12px")));
        result.addRule(getCssRule("h1", Map.of("font-size", "24px", "font-weight", "bolder")));
        result.addRule(getCssRule("h2", Map.of("font-size", "14px", "font-weight", "bold")));
        result.addRule(getCssRule("table", Map.of("border-spacing", "0")));
        result.addRule(getCssRule("td", Map.of("font-size", "10px", "white-space", "nowrap")));
        result.addRule(getCssRule("td.date", Map.of("text-align", "right")));
        result.addRule(getCssRule("td.description", Map.of("font-size", "8px", "white-space", "nowrap")));
        result.addRule(getCssRule("td.amount", Map.of("color","#000000", "text-align", "right")));
        result.addRule(getCssRule("td.amount-debit", Map.of("color","#FF0000")));
        result.addRule(getCssRule("td.center-column", Map.of("border-right","2px solid darkblue", "width", "10px")));
        result.addRule(getCssRule("th.total-column", Map.of("padding-left", "30px")));
        result.addRule(getCssRule("td.total-row", Map.of("border-top", "2px solid black", "padding-top","4px","font-size", "14px","font-weight", "bold")));
        result.addRule(getCssRule("img.pie", Map.of("display", "block", "margin-left","auto","margin-right","auto")));

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
        Element title = new Element("title")
                .setContent(new Text("Report"));

        Element style = new Element("style")
                .setContent(new Text(getStyleSheet()));

        return new Element("head")
                .addContent(title)
                .addContent(style);
    }

    private static void addDateToRow(Element row, LocalDate transactionDate) {
        Element date = new Element("td");
        date.setAttribute("class","date");
        date.setText(DateTimeFormatter.ofPattern("dd-MMM").format(transactionDate) +
                "<br/>" +
                DateTimeFormatter.ofPattern("yyyy").format(transactionDate));
        row.addContent(date);
    }

    private static void addImageToRow(Element row, String imagePath, String imageName) {
        Element column = new Element("td");
        Element image = new Element("img");
        image.setAttribute("height","25px");
        image.setAttribute("width","25px");
        image.setAttribute("src", imagePath + imageName + ".png");
        column.addContent(image);
        row.addContent(column);
    }

    private static void addDescriptionToRow(Element row, String description) {
        // Split the string into lines, break at words.
        String[] lines = WordUtils.wrap(description,30,"\n",true," ").split("\n");

        // Split that into separate string.
        Element descriptionElement = new Element("td")
                .setAttribute("class","description");

        if(lines.length <= 1) {
            descriptionElement.setText(lines[0]);
        } else {
            descriptionElement.setText(lines[0] + "<br/>" + lines[1]);
        }

        row.addContent(descriptionElement);
    }

    private static void addAmountToRow(Element row, FinancialAmount amount, String positiveClass, String negativeClass) {
        row.addContent(new Element("td")
                .setAttribute("class", amount.getValue() < 0 ? negativeClass : positiveClass)
                .setText(amount.toString()));
    }

    private static void addTransactionToRow(Element row, Transaction transaction, String imagePath) {
        if(transaction == null) {
            // Add blank columns
            for(int i = 0; i < 5; i++) {
                row.addContent(new Element("td"));
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
        addAmountToRow(row,transaction.getAmount(),"amount","amount amount-debit");
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
        Element result = new Element("table");

        // Add the title
        result.addContent(new Element("tr")
                .addContent(new Element("th").setText("Date"))
                .addContent(new Element("th").setText(""))
                .addContent(new Element("th").setText(""))
                .addContent(new Element("th").setText("Description"))
                .addContent(new Element("th").setText("Amount"))
                .addContent(new Element("th").setText(""))
                .addContent(new Element("th").setText("Date"))
                .addContent(new Element("th").setText(""))
                .addContent(new Element("th").setText(""))
                .addContent(new Element("th").setText("Description"))
                .addContent(new Element("th").setText("Amount")));

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
            Element row = new Element("tr");

            addTransactionToRow(row,nextPair.getLeft(),this.workingDirectory);

            Element centerColumn = new Element("td");
            centerColumn.setAttribute("class", "center-column");
            row.addContent(centerColumn);

            addTransactionToRow(row,nextPair.getRight(),this.workingDirectory);

            result.addContent(row);
        }

        return result;
    }

    private Element getComparisonRow(String categoryId, CategoryComparison categoryComparison) {
        Element comparisonRow = new Element("tr");

        // Image and name
        addImageToRow(comparisonRow,workingDirectory,categoryId);
        comparisonRow.addContent(new Element("td").setText(categoryComparison.getCategory().getName()));

        // Amounts
        addAmountToRow(comparisonRow,categoryComparison.getThisMonth(),"amount","amount amount-debit");
        addAmountToRow(comparisonRow,categoryComparison.getPreviousMonth(),"amount","amount amount-debit");

        // Percentage change, if anything.
        if(categoryComparison.getPreviousMonth().getValue() != 0.0 && categoryComparison.getPercentageChange() != 0.0) {
            comparisonRow.addContent(new Element("td")
                    .setAttribute("class", categoryComparison.getPercentageChange() < 0 ? "amount amount-debit" : "amount")
                    .setText(new DecimalFormat("#").format(categoryComparison.getPercentageChange()) + "%"));
        } else {
            comparisonRow.addContent(new Element("td"));
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
        Element result = new Element("table");

        // Add the header.
        result.addContent(new Element("tr")
                .addContent(new Element("th"))
                .addContent(new Element("th"))
                .addContent(new Element("th").setAttribute("class","total-column").setText("Current Spend"))
                .addContent(new Element("th").setAttribute("class","total-column").setText(getPreviousTitle(month)))
                .addContent(new Element("th").setAttribute("class","total-column").setText("Change in Spend")));

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
        Element totalRow = new Element("tr");
        result.addContent(totalRow);

        totalRow
            .addContent(new Element("td"))
            .addContent(new Element("td")
                .setAttribute("class","total-row")
                .setText("Total"));
        addAmountToRow(totalRow, totalThis,"total-row amount","total-row amount amount-debit");
        addAmountToRow(totalRow, totalPrevious,"total-row amount","total-row amount amount-debit");

        double totalPercentage = 0.0;
        if(totalPrevious.getValue() != 0.0) {
            totalPercentage = ((totalThis.getValue() - totalPrevious.getValue()) / totalPrevious.getValue()) * 100.0;
        }

        // Add the percentage total.
        if(totalPrevious.getValue() != 0.0 && totalPercentage != 0.0) {
            totalRow.addContent(new Element("td")
                    .setAttribute("class", totalPercentage < 0 ? "total-row amount amount-debit" : "total-row amount")
                    .setText(new DecimalFormat("#").format(totalPercentage) + "%"));
        } else {
            totalRow.addContent(new Element("td"));
        }

        return result;
    }

    private Element getMonthReportBody() {
        Element titleText = new Element("h1")
                .addContent(DateTimeFormatter.ofPattern("MMMM yyyy").format(this.reportDate));

        Element pie = new Element("img")
                .setAttribute("class", "pie")
                .setAttribute("height", "400px")
                .setAttribute("width", "400px")
                .setAttribute("src", this.workingDirectory + "/pie-.png");

        Element pageBreak = new Element("p")
                .setAttribute("style", "page-break-after: always;")
                .setText("&#xA0;");

        return new Element(BODY)
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
        Element titleText = new Element("h1")
                .addContent(DateTimeFormatter.ofPattern("yyyy").format(this.reportDate) + " Summary");

        Element pie = new Element("img")
                .setAttribute("class", "pie")
                .setAttribute("height", "400px")
                .setAttribute("width", "400px")
                .setAttribute("src", this.workingDirectory + "/pie-yr.png");

        Element body = new Element(BODY)
                .addContent(titleText)
                .addContent(pie)
                .addContent(getComparisonTable(this.transactions, this.previousTransactions,false));

        for(int i = 0; i < 12; i++) {
            body.addContent(new Element("p")
                    .setAttribute("style", "page-break-after: always;")
                    .setText("&#xA0;"));
            body.addContent(new Element("h1")
                    .addContent(DateTimeFormatter.ofPattern("MMMM yyyy").format(LocalDate.of(this.reportDate.getYear(),i + 1,1))));
            body.addContent(new Element("img")
                    .setAttribute("class", "pie")
                    .setAttribute("height", "400px")
                    .setAttribute("width", "400px")
                    .setAttribute("src", this.workingDirectory + "/pie-" + i + ".png"));

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
