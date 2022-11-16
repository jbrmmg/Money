package com.jbr.middletier.money.control;

import com.itextpdf.text.DocumentException;
import com.jbr.middletier.money.dto.ArchiveOrReportRequestDTO;
import com.jbr.middletier.money.dto.StatusDTO;
import com.jbr.middletier.money.manager.ArchiveManager;
import com.jbr.middletier.money.reporting.ReportGenerator;
import org.apache.batik.transcoder.TranscoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@Controller
@RequestMapping("/jbr")
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

    @PostMapping(path="/int/money/transaction/archive")
    public @ResponseBody StatusDTO archive(@RequestBody ArchiveOrReportRequestDTO archiveRequest) {
        this.archiveManager.archive(archiveRequest);

        return StatusDTO.OK;
    }

    @PostMapping(path="/int/money/transaction/report")
    public @ResponseBody StatusDTO report(@RequestBody ArchiveOrReportRequestDTO report) throws TranscoderException, DocumentException, IOException {
        LOG.info("Report Controller - request report.");
        reportGenerator.generateReport(report.getYear(),report.getMonth());

        return StatusDTO.OK;
    }

    @PostMapping(path="/int/money/transaction/annualreport")
    public @ResponseBody StatusDTO annualReport(@RequestBody ArchiveOrReportRequestDTO report) throws TranscoderException, DocumentException, IOException {
        LOG.info("Report Controller - request report (Annual).");
        reportGenerator.generateAnnualReport(report.getYear());

        return StatusDTO.OK;
    }
}
