package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.OkStatus;
import com.jbr.middletier.money.dto.EmailRequestDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.reporting.EmailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jbr")
public class EmailController {
    private static final Logger LOG = LoggerFactory.getLogger(EmailController.class);

    private final EmailGenerator emailGenerator;

    @Autowired
    public EmailController(EmailGenerator emailGenerator) {
        this.emailGenerator = emailGenerator;
    }

    @PostMapping(path="/int/money/email")
    public OkStatus sendEmail(@RequestBody EmailRequestDTO request) throws EmailGenerationException {
        LOG.info("sending email to (sanitized) {}", request);
        this.emailGenerator.generateReport(request);

        return OkStatus.getOkStatus();
    }
}
