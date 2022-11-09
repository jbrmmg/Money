package com.jbr.middletier.money.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Component
@Profile("!emailtest")
public class TransportWrapperImpl implements TransportWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransportWrapperImpl.class);

    // Wrapper to help with unit testing.
    public void sendEmail(Message message) throws MessagingException {
        LOG.info("Send email");
        Transport.send(message);
    }
}
