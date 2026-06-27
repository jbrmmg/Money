package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.primary.OkStatus;
import com.jbr.middletier.money.dto.EmailRequestDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.reporting.EmailGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Generate and send a financial summary email")
    @PostMapping(path="/email")
    public OkStatus sendEmail(@Valid @RequestBody EmailRequestDTO request) throws EmailGenerationException {
        LOG.info("sending email to (sanitized) {}", request);
        this.emailGenerator.generateReport(request);
        return OkStatus.getOkStatus();
    }
}
