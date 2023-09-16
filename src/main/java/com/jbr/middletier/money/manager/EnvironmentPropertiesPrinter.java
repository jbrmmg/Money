package com.jbr.middletier.money.manager;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentPropertiesPrinter {
    private static final Logger LOG = LoggerFactory.getLogger(EnvironmentPropertiesPrinter.class);

    private final Environment env;

    @Autowired
    public EnvironmentPropertiesPrinter(Environment env) {
        this.env = env;
    }

    @PostConstruct
    public void logApplicationProperties() {
        LOG.info("{}={}", "spring.datasource.url", env.getProperty("spring.datasource.url"));
        LOG.info("{}={}", "spring.datasource.username", env.getProperty("spring.datasource.username"));
    }
}
