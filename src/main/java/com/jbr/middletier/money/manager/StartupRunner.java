package com.jbr.middletier.money.manager;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(StartupRunner.class);

    private final StatementManager statementManager;

    @Autowired
    StartupRunner(StatementManager statementManager) {
        this.statementManager = statementManager;
    }

    @Override
    public void run(String... args) {
        LOG.info("Update the statement ages.");

        // Update the statement ages in the database.
        this.statementManager.checkStatementAges();
    }
}
