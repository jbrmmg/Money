package com.jbr.middletier.money.xml.html;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.util.FinancialAmount;
import org.jdom2.Element;
import org.jdom2.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmailHtml extends HyperTextMarkupLanguage {
    private static final String TABLE_HEADER = "th";
    private static final String TABLE_ROW = "tr";
    private static final String TABLE = "table";
    private static final String BODY = "body";
    private static final String CLASS = "class";
    private static final String PADDING = "padding";
    private static final String DESCRIPTION = "description";

    private final FinancialAmount start;
    private final List<Transaction> transactions;

    private CSSStyleRule getDateRule() {
        CSSStyleRule fillRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(".date");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        fillRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration(PADDING, CSSExpression.createSimple("2px 4px 0 0"));
        fillRule.addDeclaration(declaration);

        return fillRule;
    }

    private CSSStyleRule getBodyRule() {
        CSSStyleRule bodyRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("body");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        bodyRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("font-family", CSSExpression.createSimple("\"Courier New\", Courier, monospace"));
        bodyRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("font-size", CSSExpression.createSimple("10px"));
        bodyRule.addDeclaration(declaration);

        return bodyRule;
    }

    private CSSStyleRule getThRule() {
        CSSStyleRule thRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("th");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        thRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("text-align", CSSExpression.createSimple("left"));
        thRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("border-bottom", CSSExpression.createSimple("2px solid black"));
        thRule.addDeclaration(declaration);

        return thRule;
    }

    private CSSStyleRule getDescriptionRule() {
        CSSStyleRule descriptionRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(DESCRIPTION);
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        descriptionRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration(PADDING, CSSExpression.createSimple("2px 4px 0 0"));
        descriptionRule.addDeclaration(declaration);

        return descriptionRule;
    }

    private CSSStyleRule getAmountRule() {
        CSSStyleRule amountRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(".amount");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        amountRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration(PADDING, CSSExpression.createSimple("2px 0 0 0"));
        amountRule.addDeclaration(declaration);

        return amountRule;
    }

    private CSSStyleRule getAmountDataRule() {
        CSSStyleRule amountDataRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(".amount-data");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        amountDataRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("text-align", CSSExpression.createSimple("right"));
        amountDataRule.addDeclaration(declaration);

        return amountDataRule;
    }

    private CSSStyleRule getDbRule() {
        CSSStyleRule dbRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(".db");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        dbRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("color", CSSExpression.createSimple("#FF0000"));
        dbRule.addDeclaration(declaration);

        return dbRule;
    }

    private CascadingStyleSheet generateCSS() {
        CascadingStyleSheet result = new CascadingStyleSheet();

        result.addRule(getBodyRule());
        result.addRule(getThRule());
        result.addRule(getDescriptionRule());
        result.addRule(getDateRule());
        result.addRule(getAmountRule());
        result.addRule(getDbRule());
        result.addRule(getAmountDataRule());

        return result;
    }

    private String getStyleSheet() {
        CSSWriterSettings settings = new CSSWriterSettings(ECSSVersion.CSS30, false);
        settings.setRemoveUnnecessaryCode(true);
        CSSWriter cssWriter = new CSSWriter(settings);

        return cssWriter.getCSSAsString(generateCSS());
    }

    protected Element getHeader() {
        Element title = new Element("title")
                .setContent(new Text("Email"));

        Element style = new Element("style")
                .setContent(new Text(getStyleSheet()));

        return new Element("head")
                .addContent(title)
                .addContent(style);
    }

    private Element createTdElement() {
        return new Element("td");
    }

    private Element getDateColumn(LocalDate date) {
        Element result = createTdElement()
                .setAttribute(CLASS,"date");

        if(null == date) {
            return result;
        }

        return result.addContent(new Text(DateTimeFormatter.ofPattern("dd/MMM").format(date)));
    }

    private Element getAccountColumn(Account account) {
        Element result = createTdElement()
                .setAttribute(CLASS,DESCRIPTION);

        if(null == account) {
            return result;
        }

        return result.addContent(new Text(account.getId()));
    }

    private Element getCategoryColumn(Category category) {
        Element result = createTdElement()
                .setAttribute(CLASS,DESCRIPTION);

        if(null == category) {
            return result;
        }

        return result.addContent(new Text(category.getName()));
    }

    private Element getDescriptionColumn(String description) {
        return createTdElement()
                .setAttribute(CLASS, DESCRIPTION)
                .addContent(new Text(description));
    }

    private Element getAmountColumn(FinancialAmount amount) {
        return createTdElement()
                .setAttribute(CLASS, amount.isNegative() ? "amount amount-data db" : "amount amount-data")
                .addContent(new Text(amount.toAbsString()));
    }

    private Element createRow(LocalDate date, Category category, Account account, String description, FinancialAmount amount) {
        return new Element(TABLE_ROW)
                .addContent(getDateColumn(date))
                .addContent(getCategoryColumn(category))
                .addContent(getAccountColumn(account))
                .addContent(getDescriptionColumn(description))
                .addContent(getAmountColumn(amount));
    }

    private Element createRow(Transaction transaction) {
        return createRow(transaction.getDate(),
                transaction.getCategory(),
                transaction.getAccount(),
                transaction.getDescription(),
                transaction.getAmount());
    }

    protected Element getBody() {
        Element headerText = new Element("p")
                .addContent(new Text("Credit card transactions up to today."));

        Element tableHeaderDate = new Element(TABLE_HEADER)
                .addContent(new Text("Date"));

        Element tableHeaderCategory = new Element(TABLE_HEADER)
                .addContent(new Text("Category"));

        Element tableHeaderAccount = new Element(TABLE_HEADER)
                .addContent(new Text("Account"));

        Element tableHeaderDescription = new Element(TABLE_HEADER)
                .addContent(new Text("Description"));

        Element tableHeaderAmount = new Element(TABLE_HEADER)
                .addContent(new Text("Amount"));

        Element tableHeader = new Element(TABLE_ROW)
                .addContent(tableHeaderDate)
                .addContent(tableHeaderCategory)
                .addContent(tableHeaderAccount)
                .addContent(tableHeaderDescription)
                .addContent(tableHeaderAmount);

        Element table = new Element(TABLE)
                .addContent(tableHeader);

        FinancialAmount endBalance = this.start;
        table.addContent(createRow(null, null, null, "Brought Forward", this.start));
        for(Transaction next : this.transactions) {
            table.addContent(createRow(next));
            endBalance.increment(next.getAmount());
        }
        table.addContent(createRow(null, null, null, "Carried Forward", endBalance));

        return new Element(BODY)
                .addContent(headerText)
                .addContent(table);
    }

    public EmailHtml(FinancialAmount startBalance, List<Transaction> transactions) {
        super();

        this.start = startBalance;
        this.transactions = transactions;
    }
}
