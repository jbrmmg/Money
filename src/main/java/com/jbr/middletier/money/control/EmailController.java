package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.OkStatus;
import com.jbr.middletier.money.reporting.EmailGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.mail.MessagingException;

@Controller
@RequestMapping("/jbr")
public class EmailController {
    private static final Logger LOG = LoggerFactory.getLogger(EmailController.class);

    private final EmailGenerator emailGenerator;

    @Autowired
    public EmailController(EmailGenerator emailGenerator) {
        this.emailGenerator = emailGenerator;
    }

    @PostMapping(path="/int/money/email")
    public @ResponseBody OkStatus sendEmail(@RequestParam(value="to", defaultValue="jason@jbrmmg.me.uk") String to,
                                            @RequestParam(value="from", defaultValue="creditcards@jbrmmg.me.uk") String from,
                                            @RequestParam(value="username", defaultValue="creditcards@jbrmmg.me.uk") String username,
                                            @RequestParam(value="host", defaultValue="smtp.ionos.co.uk") String host,
                                            @RequestParam(value="password") String password,
                                            @RequestParam(value="weeks", defaultValue="6") int weeks ) throws Exception {
        // TODO change the exception to be specific
        LOG.info("sending email to {}", to);

        try {
            this.emailGenerator.generateReport(to,from,username,host,password,weeks);
        } catch(MessagingException e) {
            throw e;
        }

        return OkStatus.getOkStatus();
    }
}
