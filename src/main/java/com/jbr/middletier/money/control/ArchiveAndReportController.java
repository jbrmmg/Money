package com.jbr.middletier.money.control;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.ArchiveOrReportRequest;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.manage.WebLogManager;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Calendar;

@Controller
@RequestMapping("/jbr")
public class ArchiveAndReportController {
    final static private Logger LOG = LoggerFactory.getLogger(ArchiveAndReportController.class);

    private final WebLogManager webLogManager;
    private final ReportGenerator reportGenerator;
    private final StatementRepository statementRepository;
    private final ApplicationProperties applicationProperties;
    private final TransactionRepository transactionRepository;

    @Autowired
    public ArchiveAndReportController(WebLogManager webLogManager,
                                      ReportGenerator reportGenerator,
                                      StatementRepository statementRepository,
                                      ApplicationProperties applicationProperties,
                                      TransactionRepository transactionRepository ) {
        this.webLogManager = webLogManager;
        this.reportGenerator = reportGenerator;
        this.statementRepository = statementRepository;
        this.applicationProperties = applicationProperties;
        this.transactionRepository = transactionRepository;
    }

    @RequestMapping(path="/int/money/transaction/archive", method= RequestMethod.POST)
    public @ResponseBody
    ArchiveOrReportRequest archive(@RequestBody ArchiveOrReportRequest archiveRequest) {
        try {
            LOG.info("Archive Controller - request archive.");

            // Find the oldest year in the database.
            long oldestYear = 100000;

            Iterable<Statement> years = statementRepository.findAll();
            for(Statement nextStatement: years) {
                if(nextStatement.getId().getYear() < oldestYear) {
                    oldestYear = nextStatement.getId().getYear();
                }
            }

            LOG.info("Oldest year - " + oldestYear);

            // Must keep at least 3 years.
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            if(oldestYear >= currentYear - 3) {
                this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to archive - too soon");
                archiveRequest.setStatus("FAILED");
                return archiveRequest;
            }

            LOG.info("Oldest year can be archived - " + oldestYear);

            // Do reports exist for this year?
            if(!reportGenerator.reportsGeneratedForYear(oldestYear)) {
                this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to archive - reports missing");
                archiveRequest.setStatus("FAILED");
                return archiveRequest;
            }

            LOG.info("About to archive - " + oldestYear);

            // Delete all transactions that are in the oldest year.
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.INFO, "Archive for this year - " + oldestYear);

            // Delete the transactions that are in this year.
            Iterable<Transaction> transactionsToDelete = transactionRepository.findByStatementIdYear((int)oldestYear);

            for (Transaction nextTransaction: transactionsToDelete) {
                transactionRepository.delete(nextTransaction);
                LOG.info("Delete transaction  - " + nextTransaction.getId());
            }

            // Delete the Statements.
            Iterable<Statement> statementsToDelete = statementRepository.findByIdYear((int)oldestYear);
            for(Statement nextStatement: statementsToDelete) {
                statementRepository.delete(nextStatement);
                LOG.info("Delete statement - " + nextStatement.getId().getYear() + " " + nextStatement.getId().getMonth());
            }

            archiveRequest.setStatus("OK");
        } catch (Exception ex) {
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to archive " + ex);
            archiveRequest.setStatus("FAILED");
        }

        return archiveRequest;
    }

    @Scheduled(cron = "#{@applicationProperties.archiveSchedule}")
    public void scheduledArchive() {
        if(!applicationProperties.getArchiveEnabled()) {
            return;
        }

        archive(new ArchiveOrReportRequest());
    }

    @RequestMapping(path="/int/money/transaction/report", method= RequestMethod.POST)
    public @ResponseBody
    ArchiveOrReportRequest report(@RequestBody ArchiveOrReportRequest report) {
        try {
            LOG.info("Report Controller - request report.");
            reportGenerator.generateReport(report.getYear(),report.getMonth());

            report.setStatus("OK");
        } catch (Exception ex) {
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to generate report " + ex);
            report.setStatus("FAILED");
        }

        return report;
    }

    @RequestMapping(path="/int/money/transaction/annualreport", method= RequestMethod.POST)
    public @ResponseBody
    ArchiveOrReportRequest annualReport(@RequestBody ArchiveOrReportRequest report) {
        try {
            LOG.info("Report Controller - request report (Annual).");
            reportGenerator.generateAnnualReport(report.getYear());

            report.setStatus("OK");
        } catch (Exception ex) {
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to generate report " + ex);
            report.setStatus("FAILED");
        }

        return report;
    }
}
