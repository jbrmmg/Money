package com.jbr.middletier.money.xml.svg;

import com.jbr.middletier.money.data.Category;
import org.jdom2.Element;
import org.jdom2.Namespace;

public class CategorySvg extends ScalableVectorGraphics {
    private static final String ELEMENT_CIRCLE = "circle";
    private static final String ATTRIBUTE_CENTER_X = "cx";
    private static final String ATTRIBUTE_CENTER_Y = "cy";
    private static final String ATTRIBUTE_RADIUS = "r";
    private static final String ATTRIBUTE_STYLE = "style";

    public CategorySvg(Category category) {
        Namespace svgNamespace = Namespace.getNamespace(NAMESPACE);

        Element circle = new Element(ELEMENT_CIRCLE, svgNamespace)
                .setAttribute(ATTRIBUTE_CENTER_X,"60")
                .setAttribute(ATTRIBUTE_CENTER_Y,"52")
                .setAttribute(ATTRIBUTE_RADIUS,"44")
                .setAttribute(ATTRIBUTE_STYLE,"stroke:#006600; fill:#" + category.getColour()+";");

        Element root = new Element("svg", svgNamespace)
                .setAttribute(ATTRIBUTE_WIDTH,"100%")
                .setAttribute(ATTRIBUTE_HEIGHT,"100%")
                .setAttribute(ATTRIBUTE_VIEW_BOX,"0 0 120 120")
                .addContent(circle);

        this.svg.addContent(root);
    }
}
