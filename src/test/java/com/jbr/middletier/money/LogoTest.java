package com.jbr.middletier.money;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.primary.LogoDefinition;
import com.jbr.middletier.money.data.primary.repository.LogoDefinitionRepository;
import com.jbr.middletier.money.manager.LogoManager;
import com.jbr.middletier.money.utils.CssAssertHelper;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
import org.jdom2.*;
import org.jdom2.input.DOMBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@SpringBootTest(classes = MiddleTier.class)
class LogoTest {
    @Autowired
    private LogoManager logoManager;

    @Autowired
    private LogoDefinitionRepository logoDefinitionRepository;

    private void checkCss(List<Content> styleContent, int textSize, String textColour, String fillColour, String borderColour, String borderColour2)  {
        boolean found = false;
        for(Content next: styleContent) {
            if(next instanceof CDATA styleSheet) {
                found = true;

                CascadingStyleSheet css = CSSReader.readFromString(styleSheet.getValue(), StandardCharsets.UTF_8, ECSSVersion.CSS30);

                Map<String, Map<String, CssAssertHelper.CssAssertHelperData>> expected = new HashMap<>();
                // Check text
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "font-weight", "bold", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "font-size", textSize + "px", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "font-family", "Arial", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "line-height", "125%", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "text-align", "center", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "text-anchor", "middle", true);
                CssAssertHelper.expectCssBuilder(expected,"tspan.am", "fill", "#" + textColour, true);
                CssAssertHelper.expectCssBuilder(expected,"rect.am", "fill","#" + fillColour, true);
                CssAssertHelper.expectCssBuilder(expected,"rect.amborder", "fill","#" + borderColour, true);
                CssAssertHelper.expectCssBuilder(expected,"rect.amborder2", "fill","#" + borderColour2, true);


                CssAssertHelper.checkCss(css,expected);
            }
        }
        Assertions.assertTrue(found);
    }

    private void checkRect(Element rectangle, int width, int height, int x, int y) {
        Assertions.assertEquals(5, rectangle.getAttributesSize());
        Assertions.assertEquals(Integer.toString(width), rectangle.getAttribute("width").getValue());
        Assertions.assertEquals(Integer.toString(height), rectangle.getAttribute("height").getValue());
        Assertions.assertEquals(Integer.toString(x), rectangle.getAttribute("x").getValue());
        Assertions.assertEquals(Integer.toString(y), rectangle.getAttribute("y").getValue());
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

        Assertions.assertEquals("100", root.getAttribute("height").getValue());
        Assertions.assertEquals("100", root.getAttribute("width").getValue());
        Assertions.assertEquals("0 0 100 100", root.getAttribute("viewBox").getValue());

        // Check the style sheet.
        Element style = root.getChild("style", namespace);
        checkCss(style.getContent(), textSize, textColour, fillColour, borderColour, borderColour2 == null ? "FFFFFF" : borderColour2);

        // Check the text element.
        Element text = root.getChild("text", namespace);
        Element tspan = text.getChild("tspan", namespace);
        Text logoText = (Text)tspan.getContent(0);
        Assertions.assertEquals(expectedLogoText,logoText.getText());

        // Check the other elements.
        boolean rectAmBorderFound = false;
        boolean rectAmBorder2Found = false;
        boolean rectAmFound = false;
        for(Element nextElement : root.getChildren()) {
            switch (nextElement.getName()) {
                case "style", "text" -> {
                    // nothing to do for these elements.
                }
                // Already checked.
                case "rect" -> {
                    // Check the rectangles.
                    switch (nextElement.getAttribute("class").getValue()) {
                        case "amborder" -> {
                            checkRect(nextElement, 100, 100, 0, 0);
                            rectAmBorderFound = true;
                        }
                        case "amborder2" -> {
                            checkRect(nextElement, 90, 90, 5, 5);
                            rectAmBorder2Found = true;
                        }
                        case "am" -> {
                            if (borderColour2 == null) {
                                checkRect(nextElement, 90, 90, 5, 5);
                            } else {
                                checkRect(nextElement, 80, 80, 10, 10);
                            }
                            rectAmFound = true;
                        }
                        default -> Assertions.fail();
                    }
                }
                default -> Assertions.fail();
            }
        }
        Assertions.assertTrue(rectAmBorderFound);
        Assertions.assertTrue(rectAmFound);
        if(borderColour2 != null) {
            Assertions.assertTrue(rectAmBorder2Found);
        }
    }

    @Test
    void testLogoGenerationDefault() throws IOException, SAXException, ParserConfigurationException {
        // Check the properties of logo definition
        LogoDefinition logoDefinition = new LogoDefinition();
        logoDefinition.setLogoText("Test");
        logoDefinition.setFontSize(10);
        logoDefinition.setFillColour("FFFFFF");
        logoDefinition.setBorderColour("FFFFFE");
        logoDefinition.setBorderTwoColour("FFFFFD");
        logoDefinition.setSecondBorder(true);
        logoDefinition.setTextColour("FFFFFC");
        Assertions.assertTrue(logoDefinition.getSecondBorder());
        Assertions.assertEquals("Test", logoDefinition.getLogoText());
        Assertions.assertEquals("FFFFFF", logoDefinition.getFillColour());
        Assertions.assertEquals("FFFFFE", logoDefinition.getBorderColour());
        Assertions.assertEquals("FFFFFD", logoDefinition.getBorderTwoColour());
        Assertions.assertEquals("FFFFFC", logoDefinition.getTextColour());
        Assertions.assertEquals(10, logoDefinition.getFontSize().longValue());

        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("XYFS", false);
        Assertions.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),32, "FFFFFF", "656565", "FFFFFF", null, "UNK");
    }

    @Test
    void testLogoGenerationJLPC() throws IOException, ParserConfigurationException, SAXException {
        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("JLPC", true);
        Assertions.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),75, "5C5C5C", "003B25", "5C5C5C", null, "jl");
    }

    @Test
    void testLogoGenerationNWDE() throws IOException, ParserConfigurationException, SAXException {
        ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("NWDE", false);
        Assertions.assertNotNull(logo);
        checkLogoString(logo.getSvgAsString(),48, "FFFFFF", "004A8F", "FFFFFF", "ED1C24", "NW");
    }

    @Test
    void testCannotFindDefault() {
        Optional<LogoDefinition> logo = logoDefinitionRepository.findById("DFLTI");
        Assertions.assertTrue(logo.isPresent());
        logoDefinitionRepository.delete(logo.get());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> logoManager.getSvgLogoForAccount("XXXX", false));
        Assertions.assertEquals("Cannot find the default logo definition.", ex.getMessage());

        logo.ifPresent(logoDefinition -> logoDefinitionRepository.save(logoDefinition));
    }

    @Test
    void testLogoDefinition() {
        LogoDefinition logoDefinition = new LogoDefinition();
        Assertions.assertFalse(logoDefinition.getSecondBorder());
        logoDefinition.setSecondBorder(true);
        Assertions.assertTrue(logoDefinition.getSecondBorder());
        logoDefinition.setY(10);
        Assertions.assertEquals(10, logoDefinition.getY().intValue());
        logoDefinition.setId("Test");
        Assertions.assertEquals("Test", logoDefinition.getId());
    }

}
