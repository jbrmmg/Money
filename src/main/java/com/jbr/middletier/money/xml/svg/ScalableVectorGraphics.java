package com.jbr.middletier.money.xml.svg;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import com.jbr.middletier.money.data.LogoDefinition;
import org.jdom2.*;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;

public class ScalableVectorGraphics {
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

        declaration = new CSSDeclaration("text-aligh", CSSExpression.createSimple("center"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("text-anchor", CSSExpression.createSimple("middle"));
        textRule.addDeclaration(declaration);

        declaration = new CSSDeclaration("fill", CSSExpression.createSimple("#" + fillColour));
        textRule.addDeclaration(declaration);

        return textRule;
    }

    private CSSStyleRule getRectFillRule(String fillColour) {
        CSSStyleRule fillRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("rect.am");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        fillRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("fill", CSSExpression.createSimple(fillColour));
        fillRule.addDeclaration(declaration);

        return fillRule;
    }

    private CSSStyleRule getRectBorderRule(String borderColour) {
        CSSStyleRule borderRule = new CSSStyleRule();

        CSSSelectorSimpleMember selectorAttribute = new CSSSelectorSimpleMember("rect.amborder");
        CSSSelector selector = new CSSSelector();

        selector.addMember(selectorAttribute);
        borderRule.addSelector(selector);

        CSSDeclaration declaration = new CSSDeclaration("fill", CSSExpression.createSimple(borderColour));
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

    public void test(LogoDefinition logoDefinition) throws IOException {
        Document svg = new Document();

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

        Element rectangle = new Element("rect", svgNamespace)
                .setAttribute("class","amborder")
                .setAttribute("width","100")
                .setAttribute("height","100")
                .setAttribute("x","0")
                .setAttribute("y","0");

        Element rectangle2 = new Element("rect", svgNamespace)
                .setAttribute("class","am")
                .setAttribute("width","90")
                .setAttribute("height","90")
                .setAttribute("x","5")
                .setAttribute("y","5");

        Content content = new Text(logoDefinition.getLogoText());

        Element tspan = new Element("tspan", svgNamespace)
                .setAttribute("class", "am")
                .setAttribute("x", "50")
                .setAttribute("y", logoDefinition.getY().toString())
                .setContent(content);

        Element text = new Element("text", svgNamespace)
                .addContent(tspan);

        Element root = new Element("svg", svgNamespace)
                .setAttribute("width","100")
                .setAttribute("height","100")
                .setAttribute("viewBox","0 0 100 100")
                .addContent(style)
                .addContent(rectangle)
                .addContent(rectangle2)
                .addContent(text);

        svg.addContent(root);

        XMLOutputter outputter = new XMLOutputter(Format.getCompactFormat());
        outputter.output(svg,System.out);
    }
}
