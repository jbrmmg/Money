package com.jbr.middletier.money.xml.svg;

import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class ScalableVectorGraphics {
    protected final Document svg;
    private final boolean omitDeclaration;

    public static final String NAMESPACE = "http://www.w3.org/2000/svg";
    protected static final String ATTRIBUTE_WIDTH = "width";
    protected static final String ATTRIBUTE_HEIGHT = "height";
    protected static final String ATTRIBUTE_VIEW_BOX = "viewBox";

    protected ScalableVectorGraphics(boolean omitDeclaration) {
        this.svg = new Document();
        this.omitDeclaration = omitDeclaration;
    }

    public String getSvgAsString() {
        XMLOutputter output = new XMLOutputter(Format.getCompactFormat().setOmitDeclaration(this.omitDeclaration));
        return output.outputString(this.svg);
    }
}
