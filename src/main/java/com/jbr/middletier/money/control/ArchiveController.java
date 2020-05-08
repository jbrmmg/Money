package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.ArchiveRequest;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.manage.WebLogManager;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/jbr")
public class ArchiveController {
    final static private Logger LOG = LoggerFactory.getLogger(ArchiveController.class);

    private final WebLogManager webLogManager;
    private final ReportGenerator reportGenerator;
    private final StatementRepository statementRepository;

    @Autowired
    public ArchiveController(WebLogManager webLogManager,
                             ReportGenerator reportGenerator,
                             StatementRepository statementRepository) {
        this.webLogManager = webLogManager;
        this.reportGenerator = reportGenerator;
        this.statementRepository = statementRepository;
    }

    @RequestMapping(path="/int/money/transaction/archive", method= RequestMethod.POST)
    public @ResponseBody
    ArchiveRequest  archive(@RequestBody ArchiveRequest archiveRequest) {
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

            reportGenerator.generateReport(2018,5);

            archiveRequest.setStatus("OK");
        } catch (Exception ex) {
            this.webLogManager.postWebLog(WebLogManager.webLogLevel.ERROR, "Failed to archive " + ex);
            archiveRequest.setStatus("FAILED");
        }

        return archiveRequest;
    }
}
