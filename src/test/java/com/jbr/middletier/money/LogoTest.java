package com.jbr.middletier.money;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.*;
import com.helger.css.reader.CSSReader;
import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.Account;
import com.jbr.middletier.money.data.Category;
import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.LogoDefinitionRepository;
import com.jbr.middletier.money.manager.LogoManager;
import com.jbr.middletier.money.utils.CssAssertHelper;
import com.jbr.middletier.money.xml.svg.CategorySvg;
import com.jbr.middletier.money.xml.svg.PieChartSvg;
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
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class LogoTest {
    @Autowired
    private LogoManager logoManager;

    @Autowired
    private LogoDefinitionRepository logoDefinitionRepository;

    private void checkCss(List<Content> styleContent, int textSize, String textColour, String fillColour, String borderColour, String borderColour2)  {
        boolean found = false;
        for(Content next: styleContent) {
            if(next instanceof CDATA) {
                found = true;

                CDATA styleSheet = (CDATA)next;

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
        Assert.assertTrue(found);
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
    public void testCannotFindDefault() {
        Optional<LogoDefinition> logo = logoDefinitionRepository.findById("DFLTI");
        Assert.assertTrue(logo.isPresent());
        logoDefinitionRepository.delete(logo.get());

        try {
            logoManager.getSvgLogoForAccount("XXXX", false);
            Assert.fail();
        } catch(IllegalStateException ex) {
            Assert.assertEquals("Cannot find the default logo definition.", ex.getMessage());
        }

        logo.ifPresent(logoDefinition -> logoDefinitionRepository.save(logoDefinition));
    }

    @Test
    public void testLogoDefinition() {
        LogoDefinition logoDefinition = new LogoDefinition();
        Assert.assertFalse(logoDefinition.getSecondBorder());
        logoDefinition.setSecondBorder(true);
        Assert.assertTrue(logoDefinition.getSecondBorder());
        logoDefinition.setY(10);
        Assert.assertEquals(10, logoDefinition.getY().intValue());
        logoDefinition.setId("Test");
        Assert.assertEquals("Test", logoDefinition.getId());
    }

    @Test
    public void testCategory() throws ParserConfigurationException, IOException, SAXException {
        Category category = new Category();
        category.setColour("564389");

        ScalableVectorGraphics categorySvg = new CategorySvg(category);
        Assert.assertNotNull(categorySvg);

        String xml = categorySvg.getSvgAsString();

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));
        org.w3c.dom.Document  document = db.parse(is);

        Document domDocument = new DOMBuilder().build(document);
        Element root = domDocument.getRootElement();

        Assert.assertEquals("100%", root.getAttribute("height").getValue());
        Assert.assertEquals("100%", root.getAttribute("width").getValue());
        Assert.assertEquals("0 0 120 120", root.getAttribute("viewBox").getValue());

        for(Element nextElement : root.getChildren()) {
            Assert.assertEquals("circle", nextElement.getName());

            Assert.assertEquals("60", nextElement.getAttribute("cx").getValue());
            Assert.assertEquals("52", nextElement.getAttribute("cy").getValue());
            Assert.assertEquals("44", nextElement.getAttribute("r").getValue());
            Assert.assertEquals("stroke:#006600; fill:#564389;", nextElement.getAttribute("style").getValue());
        }
    }

    @Test
    public void testPieChart() throws ParserConfigurationException, IOException, SAXException {
        Category categoryFDG = new Category();
        categoryFDG.setId("FDG");
        categoryFDG.setSystemUse(false);
        categoryFDG.setColour("FFFF00");
        categoryFDG.setExpense(true);

        Category categoryHSE = new Category();
        categoryHSE.setId("HSE");
        categoryHSE.setSystemUse(false);
        categoryHSE.setColour("9966FF");
        categoryHSE.setExpense(true);

        Account account = new Account();
        account.setId("BANK");

        Transaction transaction1 = new Transaction();
        transaction1.setCategory(categoryHSE);
        transaction1.setAccount(account);
        transaction1.setAmount(-10.02);
        transaction1.setDescription("Test");

        Transaction transaction2 = new Transaction();
        transaction2.setCategory(categoryHSE);
        transaction2.setAccount(account);
        transaction2.setAmount(-210.02);
        transaction2.setDescription("Test");

        Transaction transaction3 = new Transaction();
        transaction3.setCategory(categoryFDG);
        transaction3.setAccount(account);
        transaction3.setAmount(-84.12);
        transaction3.setDescription("Test");

        List<Transaction> transactions = new ArrayList<>();
        transactions.add(transaction1);
        transactions.add(transaction2);
        transactions.add(transaction3);

        ScalableVectorGraphics pieChartSvg = new PieChartSvg(transactions);
        String pieChart = pieChartSvg.getSvgAsString();

        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(pieChart));
        org.w3c.dom.Document  document = db.parse(is);

        Document domDocument = new DOMBuilder().build(document);
        Element root = domDocument.getRootElement();

        Assert.assertEquals("0 0 10000 10000", root.getAttribute("viewBox").getValue());

        for(Element nextElement : root.getChildren()) {
            if(nextElement.getName().equals("circle")) {
                if(nextElement.getAttribute("id").getValue().equals("BCKG")) {
                    Assert.assertEquals("5000", nextElement.getAttribute("cx").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("cy").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("r").getValue());
                    Assert.assertEquals("white", nextElement.getAttribute("fill").getValue());
                } else if (nextElement.getAttribute("id").getValue().equals("FDG")) {
                    Assert.assertEquals("5000", nextElement.getAttribute("cx").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("cy").getValue());
                    Assert.assertEquals("2500", nextElement.getAttribute("r").getValue());
                    Assert.assertEquals("none", nextElement.getAttribute("fill").getValue());
                    Assert.assertEquals("#FFFF00", nextElement.getAttribute("stroke").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("stroke-width").getValue());
                    Assert.assertEquals("15707.963268 15707.963268", nextElement.getAttribute("stroke-dasharray").getValue());
                    Assert.assertEquals("rotate(-90) translate(-10000)", nextElement.getAttribute("transform").getValue());
                } else if (nextElement.getAttribute("id").getValue().equals("HSE")) {
                    Assert.assertEquals("5000", nextElement.getAttribute("cx").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("cy").getValue());
                    Assert.assertEquals("2500", nextElement.getAttribute("r").getValue());
                    Assert.assertEquals("none", nextElement.getAttribute("fill").getValue());
                    Assert.assertEquals("#9966FF", nextElement.getAttribute("stroke").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("stroke-width").getValue());
                    Assert.assertEquals("11363.690944 15707.963268", nextElement.getAttribute("stroke-dasharray").getValue());
                    Assert.assertEquals("rotate(-90) translate(-10000)", nextElement.getAttribute("transform").getValue());

                } else if (nextElement.getAttribute("id").getValue().equals("OUTL")) {
                    Assert.assertEquals("5000", nextElement.getAttribute("cx").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("cy").getValue());
                    Assert.assertEquals("5000", nextElement.getAttribute("r").getValue());
                    Assert.assertEquals("none", nextElement.getAttribute("fill").getValue());
                    Assert.assertEquals("black", nextElement.getAttribute("stroke").getValue());
                    Assert.assertEquals("20", nextElement.getAttribute("stroke-width").getValue());
                } else {
                    Assert.fail();
                }
            } else if (nextElement.getName().equals("text")) {
                if (nextElement.getAttribute("id").getValue().equals("FDG-txt")) {
                    Assert.assertEquals("#000000", nextElement.getAttribute("fill").getValue());
                    Assert.assertEquals("600px", nextElement.getAttribute("font-size").getValue());
                    Assert.assertEquals("start", nextElement.getAttribute("text-anchor").getValue());
                    Assert.assertEquals("1334.769132", nextElement.getAttribute("x").getValue());
                    Assert.assertEquals("1900.63189", nextElement.getAttribute("y").getValue());
                    Assert.assertEquals("rotate(40.218306 1334.769132,1900.63189)", nextElement.getAttribute("transform").getValue());
                } else if (nextElement.getAttribute("id").getValue().equals("HSE-txt")) {
                    Assert.assertEquals("#000000", nextElement.getAttribute("fill").getValue());
                    Assert.assertEquals("1200px", nextElement.getAttribute("font-size").getValue());
                    Assert.assertEquals("end", nextElement.getAttribute("text-anchor").getValue());
                    Assert.assertEquals("8665.230868", nextElement.getAttribute("x").getValue());
                    Assert.assertEquals("8099.36811", nextElement.getAttribute("y").getValue());
                    Assert.assertEquals("rotate(40.218306 8665.230868,8099.36811)", nextElement.getAttribute("transform").getValue());
                } else {
                    Assert.fail();
                }
            } else {
                Assert.fail();
            }
        }
    }
}
