package com.jbr.middletier.money.xml.html;

import com.helger.css.decl.*;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class HyperTextMarkupLanguage {
    protected static final String HTML_NO_BREAK_SPACE = "&#xA0;";
    protected static final String HTML_NO_BREAK_SPACE_ESC = "&amp;#xA0;";
    protected static final String HTML_BR = "<br/>";
    protected static final String HTML_BR_ESC = "&lt;br/&gt;";
    protected static final String HTML_TD = "td";
    protected static final String HTML_TH = "th";
    protected static final String HTML_TR = "tr";
    protected static final String HTML_BODY = "body";
    protected static final String HTML_TABLE = "table";
    protected static final String HTML_P = "p";
    protected static final String HTML_IMG = "img";
    protected static final String HTML_SRC_ATTRIBUTE = "src";
    protected static final String HTML_BORDER_SPACING = "border-spacing";
    protected static final String HTML_BORDER_TOP = "border-top";
    protected static final String HTML_BORDER_BOTTOM = "border-bottom";
    protected static final String HTML_BORDER_RIGHT = "border-right";
    protected static final String HTML_PADDING = "padding";
    protected static final String HTML_PADDING_TOP = "padding-top";
    protected static final String HTML_PADDING_LEFT = "padding-left";
    protected static final String HTML_STYLE = "style";
    protected static final String HTML_STYLE_H1 = "h1";
    protected static final String HTML_STYLE_H2 = "h2";
    protected static final String HTML_HEAD = "head";
    protected static final String HTML_TITLE = "title";
    protected static final String HTML_WHITESPACE = "white-space";
    protected static final String HTML_NOWRAP = "nowrap";
    protected static final String HTML_DISPLAY = "display";
    protected static final String HTML_BLOCK = "block";
    protected static final String HTML_CSS_FONT_FAMILY = "font-family";
    protected static final String HTML_CSS_FONT_SIZE = "font-size";
    protected static final String HTML_CSS_FONT_WEIGHT = "font-weight";
    protected static final String HTML_CSS_FONT_SAN_SERIF = "sans-serif";
    protected static final String HTML_CSS_FONT_MONOSPACED = "monospace";
    protected static final String HTML_CSS_WIDTH = "width";
    protected static final String HTML_CSS_CLASS = "class";
    protected static final String HTML_CSS_HEIGHT = "height";
    protected static final String HTML_CSS_MARGIN = "margin";
    protected static final String HTML_CSS_MARGIN_LEFT = "margin-left";
    protected static final String HTML_CSS_MARGIN_RIGHT = "margin-right";
    protected static final String HTML_CSS_MARGIN_AUTO = "auto";
    protected static final String HTML_CSS_BOLD = "bold";
    protected static final String HTML_CSS_BOLDER = "bolder";
    protected static final String HTML_CSS_COLOUR = "color";
    protected static final String HTML_CSS_TEXT_ALIGN = "text-align";
    protected static final String HTML_CSS_RIGHT = "right";

    protected enum UnitType {
        PX("px"),
        PT("pt");

        private final String display;

        UnitType(String display) {
            this.display = display;
        }

        @Override
        public String toString() {
            return this.display;
        }
    }

    protected static String concatenateClass(String... strings) {
        return String.join(" ", strings);
    }

    private static String quoteStringIfSpace(String value) {
        if(value.contains(" ")) {
            return "\"" + value + "\"";
        }

        return value;
    }

    protected static String fontString(String... strings) {
        return Arrays.stream(strings)
                .map(HyperTextMarkupLanguage::quoteStringIfSpace)
                .collect(Collectors.joining(", "));
    }

    protected static String borderString(String colour) {
        return concatenateClass(toUnitString(UnitType.PX, 2), "solid", colour);
    }

    protected static String selector(String parent, String child) {
        return parent + "." + child;
    }

    private static String toUnitString(UnitType unit, int value) {
        if(value == 0) {
            return String.valueOf(value);
        }

        return value + unit.toString();
    }

    protected static String formatedUnit(UnitType unit, Integer... values) {
        return Arrays.stream(values)
                .map(element -> toUnitString(unit,element))
                .collect(Collectors.joining(" "));
    }

    protected final Document html;
    protected final Element root;
    protected final Map<String,String> replacements;

    protected abstract Element getHeader();

    protected abstract Element getBody();

    protected CSSStyleRule getCssRule(String selectorName, Map<String,String> declarations) {
        CSSStyleRule centerColumnRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember(selectorName);
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        centerColumnRule.addSelector(selector);

        for(Map.Entry<String,String> nextDeclaration : declarations.entrySet()) {
            CSSDeclaration declaration = new CSSDeclaration(nextDeclaration.getKey(), CSSExpression.createSimple(nextDeclaration.getValue()));
            centerColumnRule.addDeclaration(declaration);
        }

        return centerColumnRule;
    }

    protected HyperTextMarkupLanguage() {
        this(null);
    }

    protected HyperTextMarkupLanguage(Map<String,String> replacements) {
        this.replacements = replacements;

        this.root = new Element("html")
                .setAttribute("lang","uk");

        DocType dtType = new DocType(root.getName());

        this.html = new Document(root,dtType);
    }

    private void setupHtml() {
        this.root.addContent(getHeader())
                .addContent(getBody());
    }

    private String performReplacements(String html) {
        if(this.replacements == null) {
            return html;
        }

        for(Map.Entry<String,String> next : this.replacements.entrySet()) {
            html = html.replace(next.getKey(), next.getValue());
        }

        return html;
    }

    public String getHtmlAsString() {
        setupHtml();

        XMLOutputter output = new XMLOutputter(Format.getPrettyFormat()
                .setOmitDeclaration(true));
        return performReplacements(output.outputString(this.html));
    }
}
