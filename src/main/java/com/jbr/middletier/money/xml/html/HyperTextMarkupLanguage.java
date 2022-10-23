package com.jbr.middletier.money.xml.html;

import com.helger.css.decl.*;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.util.Map;

public abstract class HyperTextMarkupLanguage {
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
