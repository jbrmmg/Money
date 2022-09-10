package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.dataaccess.LogoDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class LogoManager {
    final static private Logger LOG = LoggerFactory.getLogger(LogoManager.class);

    private final LogoDefinitionRepository logoDefinitionRepository;
    @SuppressWarnings("FieldCanBeLocal")
    private final String DEFAULT_LOGO_ID = "DFLTI";

    @Autowired
    public LogoManager(LogoDefinitionRepository logoDefinitionRepository) {
        this.logoDefinitionRepository = logoDefinitionRepository;
    }

    public static class ScalableVectorGraphics {
        private final String svgString;

        private ScalableVectorGraphics(String svgString) {
            this.svgString = svgString;
        }

        public String getSvgAsString() {
            return this.svgString;
        }
    }

    public ScalableVectorGraphics getSvgLogoForAccount(String accountId, boolean disabled) {
        LOG.info("Find logo for account id {} (disabled={})", accountId,disabled);

        // determine the logo id.
        String logoId = accountId + (disabled ? "T" : "F");

        // Get the logo definition
        Optional<LogoDefinition> logoDefinition = logoDefinitionRepository.findById(logoId);
        if(!logoDefinition.isPresent()) {
            logoDefinition = logoDefinitionRepository.findById(DEFAULT_LOGO_ID);

            if(!logoDefinition.isPresent()) {
                throw new IllegalStateException("Cannot find the default logo definition.");
            }
        }

        // Generate a logo, SVG
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        stringBuilder.append("<svg\n");
        stringBuilder.append("        width   = \"100\"\n");
        stringBuilder.append("        height  = \"100\"\n");
        stringBuilder.append("        viewBox = \"0 0 100 100\"\n");
        stringBuilder.append("        xmlns   = \"http://www.w3.org/2000/svg\">\n");
        stringBuilder.append("    <style type=\"text/css\">\n");
        stringBuilder.append("        <![CDATA[\n");
        stringBuilder.append("         tspan.am {\n");
        stringBuilder.append("            font-weight: bold;\n");
        stringBuilder.append("            font-size:   ").append(logoDefinition.get().getFontSize()).append("px;\n");
        stringBuilder.append("            line-height: 125%;\n");
        stringBuilder.append("            font-family: Arial;\n");
        stringBuilder.append("            text-align:  center;\n");
        stringBuilder.append("            text-anchor: middle;\n");
        stringBuilder.append("            fill:        #").append(logoDefinition.get().getTextColour()).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.am {\n");
        stringBuilder.append("            fill: #").append(logoDefinition.get().getFillColour()).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.amborder {\n");
        stringBuilder.append("            fill: #").append(logoDefinition.get().getBorderColour()).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("\n");
        stringBuilder.append("         rect.amborder2 {\n");
        stringBuilder.append("            fill: #").append(logoDefinition.get().getBorderTwoColour().trim()).append(";\n");
        stringBuilder.append("         }\n");
        stringBuilder.append("      ]]>\n");
        stringBuilder.append("    </style>\n");
        stringBuilder.append("    <rect class=\"amborder\" width=\"100\" height=\"100\" x=\"0\" y=\"0\"/>\n");
        if(logoDefinition.get().getSecondBorder()) {
            stringBuilder.append("    <rect class=\"amborder2\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n");
            stringBuilder.append("    <rect class=\"am\" width=\"80\" height=\"80\" x=\"10\" y=\"10\"/>\n");
        } else {
            stringBuilder.append("    <rect class=\"am\" width=\"90\" height=\"90\" x=\"5\" y=\"5\"/>\n");
        }
        stringBuilder.append("    <text>\n");
        stringBuilder.append("        <tspan class=\"am\" x=\"50\" y=\"").append(logoDefinition.get().getY()).append("\">").append(logoDefinition.get().getLogoText()).append("</tspan>\n");
        stringBuilder.append("    </text>\n");
        stringBuilder.append("</svg>");

        return new ScalableVectorGraphics(stringBuilder.toString());
    }
}
