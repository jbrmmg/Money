package com.jbr.middletier.money.manager;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Calendar;

@Controller
public class ArchiveManager {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveManager.class);

    private final StatementRepository statementRepository;
    private final ApplicationProperties applicationProperties;
    private final TransactionRepository transactionRepository;
    private final ReportGenerator reportGenerator;

    public ArchiveManager(StatementRepository statementRepository,
                          ApplicationProperties applicationProperties,
                          TransactionRepository transactionRepository,
                          ReportGenerator reportGenerator) {
        this.statementRepository = statementRepository;
        this.applicationProperties = applicationProperties;
        this.transactionRepository = transactionRepository;
        this.reportGenerator = reportGenerator;
    }

    @Scheduled(cron = "#{@applicationProperties.archiveSchedule}")
    public void scheduledArchive() {
        archive(null);
    }

    public void archive(ArchiveOrReportRequestDTO archiveRequest) {
        // If archive request is null then this is a scheduled request.
        if(archiveRequest == null && !applicationProperties.getArchiveEnabled()) {
            LOG.info("Scheduled archive is disabled.");
            return;
        }

        LOG.info("Archive Controller - request archive.");

        // Find the oldest year in the database.
        long oldestYear = 100000;

        Iterable<Statement> years = statementRepository.findAll();
        for(Statement nextStatement: years) {
            if(nextStatement.getId().getYear() < oldestYear) {
                oldestYear = nextStatement.getId().getYear();
            }
        }

        LOG.info("Oldest year - {}", oldestYear);

        // Must keep at least 3 years.
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if(oldestYear >= currentYear - 3) {
            LOG.info("Arching skipped as not enough data in the database.");
            return;
        }

        LOG.info("Oldest year can be archived - {}", oldestYear);

        // Do reports exist for this year?
        if(!reportGenerator.reportsGeneratedForYear(oldestYear)) {
            LOG.info("Arching skipped as report is not yet generated.");
            return;
        }

        LOG.info("About to archive - {}", oldestYear);

        // Delete the transactions that are in this year.
        Iterable<Transaction> transactionsToDelete = transactionRepository.findByStatementIdYear((int)oldestYear);

        for (Transaction nextTransaction: transactionsToDelete) {
            transactionRepository.delete(nextTransaction);
            LOG.info("Delete transaction  - {}", nextTransaction.getId());
        }

        // Delete the Statements.
        Iterable<Statement> statementsToDelete = statementRepository.findByIdYear((int)oldestYear);
        for(Statement nextStatement: statementsToDelete) {
            statementRepository.delete(nextStatement);
            LOG.info("Delete statement - {} {}", nextStatement.getId().getYear(), nextStatement.getId().getMonth());
        }
    }
}
