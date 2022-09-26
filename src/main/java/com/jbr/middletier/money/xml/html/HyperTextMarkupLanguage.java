package com.jbr.middletier.money.xml.html;

import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public abstract class HyperTextMarkupLanguage {
    protected final Document html;
    protected final Element root;

    protected abstract Element getHeader();

    protected abstract Element getBody();

    protected HyperTextMarkupLanguage() {
        this.root = new Element("html")
                .setAttribute("lang","uk");

        DocType dtType = new DocType(root.getName());

        this.html = new Document(root,dtType);
    }

    private void setupHtml() {
        this.root.addContent(getHeader())
                .addContent(getBody());
    }

    public String getHtmlAsString() {
        setupHtml();

        XMLOutputter output = new XMLOutputter(Format.getPrettyFormat()
                .setOmitDeclaration(true));
        return output.outputString(this.html);
    }
}
