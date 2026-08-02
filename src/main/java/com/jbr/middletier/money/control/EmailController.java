package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.primary.OkStatus;
import com.jbr.middletier.money.dto.ReportDefinitionDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.reporting.EmailGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Email", description = "Email report generation and dispatch")
public class EmailController {
    private static final Logger LOG = LoggerFactory.getLogger(EmailController.class);

    private final EmailGenerator emailGenerator;

    @Autowired
    public EmailController(EmailGenerator emailGenerator) {
        this.emailGenerator = emailGenerator;
    }

    @Operation(summary = "List available generated reports")
    @GetMapping(path = "/email/reports")
    public List<ReportDefinitionDTO> getAvailableReports() {
        return emailGenerator.getAvailableReports();
    }

    @Operation(summary = "Email a generated report to the configured recipient")
    @PostMapping(path = "/email")
    public OkStatus sendEmail(@Valid @RequestBody ReportDefinitionDTO request) throws EmailGenerationException {
        LOG.info("Sending report email: {} {}/{}", request.getType(), request.getYear(), request.getMonth());
        emailGenerator.sendReport(request);
        return OkStatus.getOkStatus();
    }
}
