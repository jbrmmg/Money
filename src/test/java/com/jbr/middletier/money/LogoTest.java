package com.jbr.middletier.money;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.manager.LogoManager;
import com.jbr.middletier.money.xml.svg.LogoSvg;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
import org.jdom2.*;
import org.jdom2.input.DOMBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class LogoTest {
    @Autowired
    private LogoManager logoManager;

    private void checkRectFill(CSSStyleRule rule, String colour) {
        Assert.assertEquals(1, rule.getDeclarationCount());
        Assert.assertEquals("#" + colour, Objects.requireNonNull(rule.getDeclarationAtIndex(0)).getExpressionAsCSSString());
        Assert.assertEquals("fill", Objects.requireNonNull(rule.getDeclarationAtIndex(0)).getProperty());
    }

    private void checkText(CSSStyleRule rule, int textSize, String colour) {
        Assert.assertEquals(7, rule.getDeclarationCount());
        boolean fontWeightFound = false;
        boolean fontSizeFound = false;
        boolean fontFamilyFound = false;
        boolean lineHeightFound = false;
        boolean textAlignFound = false;
        boolean textAnchorFound = false;
        boolean fillFound = false;
        for(int i = 0; i < rule.getDeclarationCount(); i++) {
            CSSDeclaration declaration = rule.getDeclarationAtIndex(i);
            switch(Objects.requireNonNull(declaration).getProperty()) {
                case "font-weight":
                    Assert.assertEquals("bold", declaration.getExpressionAsCSSString());
                    fontWeightFound = true;
                    break;
                case "font-size":
                    Assert.assertEquals(textSize + "px", declaration.getExpressionAsCSSString());
                    fontSizeFound = true;
                    break;
                case "font-family":
                    Assert.assertEquals("Arial", declaration.getExpressionAsCSSString());
                    fontFamilyFound = true;
                    break;
                case "line-height":
                    Assert.assertEquals("125%", declaration.getExpressionAsCSSString());
                    lineHeightFound = true;
                    break;
                case "text-align":
                    Assert.assertEquals("center", declaration.getExpressionAsCSSString());
                    textAlignFound = true;
                    break;
                case "text-anchor":
                    Assert.assertEquals("middle", declaration.getExpressionAsCSSString());
                    textAnchorFound = true;
                    break;
                case "fill":
                    Assert.assertEquals("#" + colour, declaration.getExpressionAsCSSString());
                    fillFound = true;
                    break;
                default:
                    Assert.fail();
            }
        }
        Assert.assertTrue(fontWeightFound);
        Assert.assertTrue(fontSizeFound);
        Assert.assertTrue(fontFamilyFound);
        Assert.assertTrue(lineHeightFound);
        Assert.assertTrue(textAlignFound);
        Assert.assertTrue(textAnchorFound);
        Assert.assertTrue(fillFound);
    }

    private void checkCss(List<Content> styleContent, int textSize, String textColour, String fillColour, String borderColour, String borderColour2)  {
        boolean found = false;
        boolean tspanCssFound = false;
        boolean rectAmCssFound = false;
        boolean rectAmBorderCssFound = false;
        boolean rectAmBorder2CssFound = false;
        for(Content next: styleContent) {
            if(next instanceof CDATA) {
                found = true;

                CDATA styleSheet = (CDATA)next;

                CascadingStyleSheet css = CSSReader.readFromString(styleSheet.getValue(), StandardCharsets.UTF_8, ECSSVersion.CSS30);

                for(CSSStyleRule nextRule : Objects.requireNonNull(css).getAllStyleRules()) {
                    CSSSelector selector = nextRule.getSelectorAtIndex(0);

                    Assert.assertEquals(2, Objects.requireNonNull(selector).getMemberCount());

                    CSSSelectorSimpleMember member = (CSSSelectorSimpleMember) selector.getMemberAtIndex(0);
                    CSSSelectorSimpleMember member2 = (CSSSelectorSimpleMember) selector.getMemberAtIndex(1);

                    String selectorName = Objects.requireNonNull(member).getValue() + Objects.requireNonNull(member2).getValue();

                    switch(selectorName) {
                        case "tspan.am":
                            checkText(nextRule, textSize, textColour);
                            tspanCssFound = true;
                            break;
                        case "rect.am":
                            checkRectFill(nextRule, fillColour);
                            rectAmCssFound = true;
                            break;
                        case "rect.amborder":
                            checkRectFill(nextRule, borderColour);
                            rectAmBorderCssFound = true;
                            break;
                        case "rect.amborder2":
                            Assert.assertNotNull(borderColour2);
                            checkRectFill(nextRule, borderColour2);
                            rectAmBorder2CssFound = true;
                            break;
                        default:
                            Assert.fail();
                    }
                }
            }
        }
        Assert.assertTrue(found);
        Assert.assertTrue(tspanCssFound);
        Assert.assertTrue(rectAmCssFound);
        Assert.assertTrue(rectAmBorderCssFound);
        Assert.assertTrue(rectAmBorder2CssFound);
    }

    private void checkRect(Element rectangle, int width, int height, int x, int y) {
        Assert.assertEquals(5, rectangle.getAttributesSize());
        Assert.assertEquals(Integer.toString(width), rectangle.getAttribute("width").getValue());
        Assert.assertEquals(Integer.toString(height), rectangle.getAttribute("height").getValue());
        Assert.assertEquals(Integer.toString(x), rectangle.getAttribute("x").getValue());
        Assert.assertEquals(Integer.toString(y), rectangle.getAttribute("y").getValue());
    }

    private void checkLogoString(String logo, int textSize, String textColour, String fillColour, String borderColour, String borderColour2, String expectedLogoText) throws IOException, SAXException, ParserConfigurationException {
        // First get the XML.
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(logo));
        org.w3c.dom.Document  document = db.parse(is);

        Document domDocument = new DOMBuilder().build(document);
        Element root = domDocument.getRootElement();
        Namespace namespace = root.getNamespace();

        Assert.assertEquals("100", root.getAttribute("height").getValue());
        Assert.assertEquals("100", root.getAttribute("width").getValue());
        Assert.assertEquals("0 0 100 100", root.getAttribute("viewBox").getValue());

        // Check the style sheet.
        Element style = root.getChild("style", namespace);
        checkCss(style.getContent(), textSize, textColour, fillColour, borderColour, borderColour2 == null ? "FFFFFF" : borderColour2);

        // Check the text element.
        Element text = root.getChild("text", namespace);
        Element tspan = text.getChild("tspan", namespace);
        Text logoText = (Text)tspan.getContent(0);
        Assert.assertEquals(expectedLogoText,logoText.getText());

        // Check the other elements.
        boolean rectAmBorderFound = false;
        boolean rectAmBorder2Found = false;
        boolean rectAmFound = false;
        for(Element nextElement : root.getChildren()) {
            switch (nextElement.getName()) {
                case "style":
                case "text":
                    // Already checked.
                    break;
                case "rect":
                    // Check the rectangles.
                    switch(nextElement.getAttribute("class").getValue()) {
                        case "amborder":
                            checkRect(nextElement, 100, 100, 0, 0);
                            rectAmBorderFound = true;
                            break;
                        case "amborder2":
                            checkRect(nextElement, 90, 90, 5, 5);
                            rectAmBorder2Found = true;
                            break;
                        case "am":
                            if(borderColour2 == null) {
                                checkRect(nextElement, 90, 90, 5, 5);
                            } else {
                                checkRect(nextElement, 80, 80, 10, 10);
                            }
                            rectAmFound = true;
                            break;
                        default:
                            Assert.fail();
                    }
                    break;
                default:
                    Assert.fail();
            }
        }
        Assert.assertTrue(rectAmBorderFound);
        Assert.assertTrue(rectAmFound);
        if(borderColour2 != null) {
            Assert.assertTrue(rectAmBorder2Found);
        }
    }

    @Test
    public void testLogoGenerationDefault() throws IOException, SAXException, ParserConfigurationException {
        // Check the properties of logo definition
        LogoDefinition logoDefinition = new LogoDefinition();
        logoDefinition.setLogoText("Test");
        logoDefinition.setFontSize(10);
        logoDefinition.setFillColour("FFFFFF");
        logoDefinition.setBorderColour("FFFFFE");
        logoDefinition.setBorderTwoColour("FFFFFD");
        logoDefinition.setSecondBorder(true);
        logoDefinition.setTextColour("FFFFFC");
        Assert.assertTrue(logoDefinition.getSecondBorder());
        Assert.assertEquals("Test", logoDefinition.getLogoText());
        Assert.assertEquals("FFFFFF", logoDefinition.getFillColour());
        Assert.assertEquals("FFFFFE", logoDefinition.getBorderColour());
        Assert.assertEquals("FFFFFD", logoDefinition.getBorderTwoColour());
        Assert.assertEquals("FFFFFC", logoDefinition.getTextColour());
        Assert.assertEquals(10, logoDefinition.getFontSize().longValue());

        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("XYFS", false);
        Assert.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),32, "FFFFFF", "656565", "FFFFFF", null, "UNK");
    }

    @Test
    public void testLogoGenerationJLPC() throws IOException, ParserConfigurationException, SAXException {
        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("JLPC", true);
        Assert.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),75, "5C5C5C", "003B25", "5C5C5C", null, "jl");
    }

    @Test
    public void testLogoGenerationNWDE() throws IOException, ParserConfigurationException, SAXException {
        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("NWDE", false);
        Assert.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),48, "FFFFFF", "004A8F", "FFFFFF", "ED1C24", "NW");
    }

    @Test
    public void testing() throws IOException {
        LogoDefinition logoDefinition = new LogoDefinition();
        logoDefinition.setLogoText("NW");
        logoDefinition.setFontSize(48);
        logoDefinition.setFillColour("004A8F");
        logoDefinition.setBorderColour("FFFFFF");
        logoDefinition.setBorderTwoColour("ED1C24");
        logoDefinition.setSecondBorder(true);
        logoDefinition.setTextColour("FFFFFF");
        logoDefinition.setY(66);

        ScalableVectorGraphics svg = new LogoSvg(logoDefinition);
        svg.getSvgAsString();
    }
}
