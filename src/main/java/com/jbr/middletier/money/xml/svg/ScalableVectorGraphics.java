package com.jbr.middletier.money.xml.svg;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import com.jbr.middletier.money.data.LogoDefinition;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class ScalableVectorGraphics {
    private final Document svg;

    private final static String ATTRIBUTE_CLASS = "class";
    private final static String ATTRIBUTE_WIDTH = "width";
    private final static String ATTRIBUTE_HEIGHT = "height";
    private final static String ATTRIBUTE_X = "x";
    private final static String ATTRIBUTE_Y = "y";
    private final static String ELEMENT_RECT = "rect";
    private final static String DECLARATION_FILL = "fill";

    private CSSStyleRule getTextRule(int fontSize, String fillColour) {
        CSSStyleRule textRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("tspan.am");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        textRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("font-weight", CSSExpression.createSimple("bold"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("font-size", CSSExpression.createSimple(fontSize + "px"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("line-height", CSSExpression.createSimple("125%"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("font-family", CSSExpression.createSimple("Arial"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("text-align", CSSExpression.createSimple("center"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("text-anchor", CSSExpression.createSimple("middle"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration(DECLARATION_FILL, CSSExpression.createSimple("#" + fillColour));
        textRule.addDeclaration(declaration);

        return textRule;
    }

    private CSSStyleRule getRectFillRule(String fillColour) {
        CSSStyleRule fillRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("rect.am");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        fillRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration(DECLARATION_FILL, CSSExpression.createSimple("#" + fillColour));
        fillRule.addDeclaration(declaration);

        return fillRule;
    }

    private CSSStyleRule getRectBorderRule(String borderColour) {
        CSSStyleRule borderRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("rect.amborder");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        borderRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration(DECLARATION_FILL, CSSExpression.createSimple("#" + borderColour));
        borderRule.addDeclaration(declaration);

        return borderRule;
    }

    private CascadingStyleSheet generateCSS(int fontSize, String textFillColour, String rectFillColour, String borderColour) {
        CascadingStyleSheet result = new CascadingStyleSheet();

        result.addRule(getTextRule(fontSize, textFillColour));
        result.addRule(getRectFillRule(rectFillColour));
        result.addRule(getRectBorderRule(borderColour));

        return result;
    }

    public ScalableVectorGraphics(LogoDefinition logoDefinition) {
        this.svg = new Document();

        Namespace svgNamespace = Namespace.getNamespace("http://www.w3.org/2000/svg");

        CSSWriterSettings settings = new CSSWriterSettings(ECSSVersion.CSS30, false);
        settings.setRemoveUnnecessaryCode(true);
        CSSWriter cssWriter = new CSSWriter(settings);

        CDATA styleSheet = new CDATA(cssWriter.getCSSAsString(generateCSS(logoDefinition.getFontSize(),
                logoDefinition.getTextColour(),
                logoDefinition.getFillColour(),
                logoDefinition.getBorderColour())));

        Element style = new Element("style", svgNamespace)
                .setContent(styleSheet);

        Element rectangle = new Element(ELEMENT_RECT, svgNamespace)
                .setAttribute(ATTRIBUTE_CLASS,"amborder")
                .setAttribute(ATTRIBUTE_WIDTH,"100")
                .setAttribute(ATTRIBUTE_HEIGHT,"100")
                .setAttribute(ATTRIBUTE_X,"0")
                .setAttribute(ATTRIBUTE_Y,"0");

        Element rectangleOptional = null;
        String rectangle2Size = "90";
        String rectangle2Location = "5";
        if(logoDefinition.getBorderTwoColour() != null && logoDefinition.getBorderTwoColour().trim().length() > 0) {
            rectangle2Size = "80";
            rectangle2Location = "10";

            rectangleOptional = new Element(ELEMENT_RECT, svgNamespace)
                    .setAttribute(ATTRIBUTE_CLASS,"amborder2")
                    .setAttribute(ATTRIBUTE_WIDTH,"90")
                    .setAttribute(ATTRIBUTE_HEIGHT,"90")
                    .setAttribute(ATTRIBUTE_X,"5")
                    .setAttribute(ATTRIBUTE_Y,"5");
        }


        Element rectangle2 = new Element(ELEMENT_RECT, svgNamespace)
                .setAttribute(ATTRIBUTE_CLASS,"am")
                .setAttribute(ATTRIBUTE_WIDTH,rectangle2Size)
                .setAttribute(ATTRIBUTE_HEIGHT,rectangle2Size)
                .setAttribute(ATTRIBUTE_X,rectangle2Location)
                .setAttribute(ATTRIBUTE_Y,rectangle2Location);

        Content content = new Text(logoDefinition.getLogoText());

        Element tspan = new Element("tspan", svgNamespace)
                .setAttribute(ATTRIBUTE_CLASS, "am")
                .setAttribute(ATTRIBUTE_X, "50")
                .setAttribute(ATTRIBUTE_Y, logoDefinition.getY().toString())
                .setContent(content);

        Element text = new Element("text", svgNamespace)
                .addContent(tspan);

        Element root = new Element("svg", svgNamespace)
                .setAttribute(ATTRIBUTE_WIDTH,"100")
                .setAttribute(ATTRIBUTE_HEIGHT,"100")
                .setAttribute("viewBox","0 0 100 100")
                .addContent(style)
                .addContent(rectangle);

        if(rectangleOptional != null) {
            root.addContent(rectangleOptional);
        }

        root.addContent(rectangle2);
        root.addContent(text);

        this.svg.addContent(root);
    }

    public String getSvgAsString() {
        XMLOutputter output = new XMLOutputter(Format.getCompactFormat());
        return output.outputString(this.svg);
    }
}
