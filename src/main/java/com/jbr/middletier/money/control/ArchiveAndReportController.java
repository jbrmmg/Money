package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.dto.StatusDTO;
import com.jbr.middletier.money.manager.ArchiveManager;
import com.jbr.middletier.money.reporting.ReportGenerator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Archive and Reports", description = "Transaction archiving and report generation")
public class ArchiveAndReportController {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveAndReportController.class);

    private final ReportGenerator reportGenerator;
    private final ArchiveManager archiveManager;

    @Autowired
    public ArchiveAndReportController(ReportGenerator reportGenerator,
                                      ArchiveManager archiveManager) {
        this.reportGenerator = reportGenerator;
        this.archiveManager = archiveManager;
    }

    @PostMapping(path="/transaction/archive")
    public StatusDTO archive(@Valid @RequestBody ArchiveOrReportRequestDTO archiveRequest) {
        this.archiveManager.archive(archiveRequest);
        return StatusDTO.OK;
    }

    @PostMapping(path="/transaction/report")
    public StatusDTO report(@Valid @RequestBody ArchiveOrReportRequestDTO report) throws IOException {
        LOG.info("Report Controller - request report.");
        reportGenerator.generateReport(report.getYear(),report.getMonth());
        return StatusDTO.OK;
    }

    @PostMapping(path="/transaction/annual-report")
    public StatusDTO annualReport(@Valid @RequestBody ArchiveOrReportRequestDTO report) throws IOException {
        LOG.info("Report Controller - request report (Annual).");
        reportGenerator.generateAnnualReport(report.getYear());
        return StatusDTO.OK;
    }
}
