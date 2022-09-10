package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.dataaccess.LogoDefinitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class LogoManager {
    private static final Logger LOG = LoggerFactory.getLogger(LogoManager.class);

    private final LogoDefinitionRepository logoDefinitionRepository;

    private final ResourceLoader resourceLoader;

    @SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection"})
    private static final String defaultLogoId = "DFLTI";

    @Autowired
    public LogoManager(LogoDefinitionRepository logoDefinitionRepository, ResourceLoader resourceLoader) {
        this.logoDefinitionRepository = logoDefinitionRepository;
        this.resourceLoader = resourceLoader;
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
            logoDefinition = logoDefinitionRepository.findById(defaultLogoId);

            if(!logoDefinition.isPresent()) {
                throw new IllegalStateException("Cannot find the default logo definition.");
            }
        }

        // Generate a logo, SVG
        String template;
        try {
            Resource resource = resourceLoader.getResource(logoDefinition.get().getSecondBorder() ? "classpath:html/logo-template-2.svg" : "classpath:html/logo-template.svg");
            InputStream is = resource.getInputStream();

            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(isr);

            template = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to get the SVG template from the resources");
        }

        template = template.replace("%%FONT_SIZE%%", logoDefinition.get().getFontSize().toString());
        template = template.replace("%%TEXT_COLOUR%%", logoDefinition.get().getTextColour());
        template = template.replace("%%FILL_COLOUR%%", logoDefinition.get().getFillColour());
        template = template.replace("%%BORDER_COLOUR%%", logoDefinition.get().getBorderColour());
        template = template.replace("%%BORDER_TWO_COLOUR%%", logoDefinition.get().getBorderTwoColour());
        template = template.replace("%%Y%%", logoDefinition.get().getY().toString());
        template = template.replace("%%LOGO_TEXT%%", logoDefinition.get().getLogoText());

        return new ScalableVectorGraphics(template);
    }
}
