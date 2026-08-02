package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.StatementRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.reporting.EmailGenerator;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
public class ArchiveManager {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveManager.class);

    private final StatementRepository statementRepository;
    private final ApplicationProperties applicationProperties;
    private final TransactionRepository transactionRepository;
    private final ReportGenerator reportGenerator;
    private final EmailGenerator emailGenerator;

    @Autowired
    public ArchiveManager(StatementRepository statementRepository,
                          ApplicationProperties applicationProperties,
                          TransactionRepository transactionRepository,
                          ReportGenerator reportGenerator,
                          EmailGenerator emailGenerator) {
        this.statementRepository = statementRepository;
        this.applicationProperties = applicationProperties;
        this.transactionRepository = transactionRepository;
        this.reportGenerator = reportGenerator;
        this.emailGenerator = emailGenerator;
    }

    @Scheduled(cron = "#{@applicationProperties.archiveSchedule}")
    public void scheduledArchive() {
        archive(null);
    }

    private void sendArchiveNotification(String subject, String text) {
        try {
            emailGenerator.sendNotification(subject, text);
        } catch (Exception e) {
            LOG.error("Failed to send archive notification: {}", e.getMessage());
        }
    }

    public void archive(ArchiveOrReportRequestDTO archiveRequest) {
        if (archiveRequest == null && !applicationProperties.getArchiveEnabled()) {
            LOG.info("Scheduled archive is disabled.");
            return;
        }

        LOG.info("Archive Controller - request archive.");

        int oldestYear = Integer.MAX_VALUE;

        Iterable<Statement> years = statementRepository.findAll();
        for (Statement nextStatement : years) {
            if (nextStatement.getId().getYear() < oldestYear) {
                oldestYear = nextStatement.getId().getYear();
            }
        }

        LOG.info("Oldest year - {}", oldestYear);

        int currentYear = LocalDate.now(ZoneId.systemDefault()).getYear();
        if (oldestYear >= currentYear - 5) {
            LOG.info("Arching skipped as not enough data in the database.");
            if (archiveRequest == null) {
                sendArchiveNotification("Archive Skipped",
                        "Archiving skipped - not enough data in the database.");
            }
            return;
        }

        LOG.info("Oldest year can be archived - {}", oldestYear);

        if (!reportGenerator.reportsGeneratedForYear(oldestYear)) {
            LOG.info("Arching skipped as report is not yet generated.");
            if (archiveRequest == null) {
                sendArchiveNotification("Archive Skipped",
                        "Archiving skipped - report not yet generated for " + oldestYear + ".");
            }
            return;
        }

        LOG.info("About to archive - {}", oldestYear);

        Iterable<Transaction> transactionsToDelete = transactionRepository.findByStatementIdYear(oldestYear);
        for (Transaction nextTransaction : transactionsToDelete) {
            transactionRepository.delete(nextTransaction);
            LOG.info("Delete transaction  - {}", nextTransaction.getId());
        }

        Iterable<Statement> statementsToDelete = statementRepository.findByIdYear(oldestYear);
        for (Statement nextStatement : statementsToDelete) {
            statementRepository.delete(nextStatement);
            LOG.info("Delete statement - {} {}", nextStatement.getId().getYear(), nextStatement.getId().getMonth());
        }

        if (archiveRequest == null) {
            sendArchiveNotification("Archive Complete",
                    "Archive complete - year " + oldestYear + " has been archived.");
        }
    }
}
