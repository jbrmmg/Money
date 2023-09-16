package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.data.LogoDefinition;
import com.jbr.middletier.money.dataaccess.LogoDefinitionRepository;
import com.jbr.middletier.money.xml.svg.LogoSvg;
import com.jbr.middletier.money.xml.svg.ScalableVectorGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.util.Optional;

@Controller
public class LogoManager {
    private static final Logger LOG = LoggerFactory.getLogger(LogoManager.class);

    private final LogoDefinitionRepository logoDefinitionRepository;

    @SuppressWarnings({"FieldCanBeLocal", "SpellCheckingInspection"})
    private static final String DEFAULT_LOGO_ID = "DFLTI";

    @Autowired
    public LogoManager(LogoDefinitionRepository logoDefinitionRepository) {
        this.logoDefinitionRepository = logoDefinitionRepository;
    }

    public ScalableVectorGraphics getSvgLogoForAccount(String accountId, boolean disabled) {
        LOG.info("Find logo for account id {} (disabled={})", accountId,disabled);

        // determine the logo id.
        String logoId = accountId + (disabled ? "T" : "F");

        // Get the logo definition
        Optional<LogoDefinition> logoDefinition = logoDefinitionRepository.findById(logoId);
        if(logoDefinition.isEmpty()) {
            logoDefinition = logoDefinitionRepository.findById(DEFAULT_LOGO_ID);

            if(logoDefinition.isEmpty()) {
                throw new IllegalStateException("Cannot find the default logo definition.");
            }
        }

        return new LogoSvg(logoDefinition.get());
    }
}
