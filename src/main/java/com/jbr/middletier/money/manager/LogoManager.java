package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.dataaccess.LogoDefinitionRepository;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
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

        return new ScalableVectorGraphics(logoDefinition.get());
    }
}
