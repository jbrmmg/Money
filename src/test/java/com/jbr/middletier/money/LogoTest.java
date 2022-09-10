package com.jbr.middletier.money;

import com.jbr.middletier.MiddleTier;
import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.manager.LogoManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MiddleTier.class)
public class LogoTest {
    @Autowired
    private LogoManager logoManager;

    @Test
    public void testLogoGenerationDefault() {
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

        LogoManager.ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("XYFS", false);
        Assert.assertNotNull(logo);
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<svg\n" +
                "        width   = \"100\"\n" +
                "        height  = \"100\"\n" +
                "        viewBox = \"0 0 100 100\"\n" +
                "        xmlns   = \"http://www.w3.org/2000/svg\">\n" +
                "    <style type=\"text/css\">\n" +
                "        <![CDATA[\n" +
                "         tspan.am {\n" +
                "            font-weight: bold;\n" +
                "            font-size:   32px;\n" +
                "            line-height: 125%;\n" +
                "            font-family: Arial;\n" +
                "            text-align:  center;\n" +
                "            text-anchor: middle;\n" +
                "            fill:        #FFFFFF;\n" +
                "         }\n" +
                "\n" +
                "         rect.am {\n" +
                "            fill: #656565;\n" +
                "         }\n" +
                "\n" +
                "         rect.amborder {\n" +
                "            fill: #FFFFFF;\n" +
                "         }\n" +
                "      ]]>\n" +
                "    </style>\n" +
                "    <rect class=\"amborder\" width=\"100\" height=\"100\" x=\"0\" y=\"0\"/>\n" +
                "    <rect class=\"am\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n" +
                "    <text>\n" +
                "        <tspan class=\"am\" x=\"50\" y=\"62\">UNK</tspan>\n" +
                "    </text>\n" +
                "</svg>",logo.getSvgAsString());
    }

    @Test
    public void testLogoGenerationJLPC() {
        LogoManager.ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("JLPC", true);
        Assert.assertNotNull(logo);
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<svg\n" +
                "        width   = \"100\"\n" +
                "        height  = \"100\"\n" +
                "        viewBox = \"0 0 100 100\"\n" +
                "        xmlns   = \"http://www.w3.org/2000/svg\">\n" +
                "    <style type=\"text/css\">\n" +
                "        <![CDATA[\n" +
                "         tspan.am {\n" +
                "            font-weight: bold;\n" +
                "            font-size:   75px;\n" +
                "            line-height: 125%;\n" +
                "            font-family: Arial;\n" +
                "            text-align:  center;\n" +
                "            text-anchor: middle;\n" +
                "            fill:        #5C5C5C;\n" +
                "         }\n" +
                "\n" +
                "         rect.am {\n" +
                "            fill: #003B25;\n" +
                "         }\n" +
                "\n" +
                "         rect.amborder {\n" +
                "            fill: #5C5C5C;\n" +
                "         }\n" +
                "      ]]>\n" +
                "    </style>\n" +
                "    <rect class=\"amborder\" width=\"100\" height=\"100\" x=\"0\" y=\"0\"/>\n" +
                "    <rect class=\"am\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n" +
                "    <text>\n" +
                "        <tspan class=\"am\" x=\"50\" y=\"74\">jl</tspan>\n" +
                "    </text>\n" +
                "</svg>",logo.getSvgAsString());
    }

    @Test
    public void testLogoGenerationNWDE() {
        LogoManager.ScalableVectorGraphics logo = this.logoManager.getSvgLogoForAccount("NWDE", false);
        Assert.assertNotNull(logo);
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<svg\n" +
                "        width   = \"100\"\n" +
                "        height  = \"100\"\n" +
                "        viewBox = \"0 0 100 100\"\n" +
                "        xmlns   = \"http://www.w3.org/2000/svg\">\n" +
                "    <style type=\"text/css\">\n" +
                "        <![CDATA[\n" +
                "         tspan.am {\n" +
                "            font-weight: bold;\n" +
                "            font-size:   48px;\n" +
                "            line-height: 125%;\n" +
                "            font-family: Arial;\n" +
                "            text-align:  center;\n" +
                "            text-anchor: middle;\n" +
                "            fill:        #FFFFFF;\n" +
                "         }\n" +
                "\n" +
                "         rect.am {\n" +
                "            fill: #004A8F;\n" +
                "         }\n" +
                "\n" +
                "         rect.amborder {\n" +
                "            fill: #FFFFFF;\n" +
                "         }\n" +
                "\n" +
                "         rect.amborder2 {\n" +
                "            fill: #ED1C24;\n" +
                "         }\n" +
                "      ]]>\n" +
                "    </style>\n" +
                "    <rect class=\"amborder\" width=\"100\" height=\"100\" x=\"0\" y=\"0\"/>\n" +
                "    <rect class=\"amborder2\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n" +
                "    <rect class=\"am\" width=\"80\" height=\"80\" x=\"10\" y=\"10\"/>\n" +
                "    <text>\n" +
                "        <tspan class=\"am\" x=\"50\" y=\"66\">NW</tspan>\n" +
                "    </text>\n" +
                "</svg>",logo.getSvgAsString());
    }
}
