package com.jbr.middletier.money.utils;

import com.jbr.middletier.money.util.TransportWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

@Component
@Profile("emailtest")
public class TransportWrapperImpl implements TransportWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransportWrapperImpl.class);

    @Override
    public void sendEmail(Message message) throws MessagingException {
        LOG.info("Test email send");

        // If the message has a host property set to throw then throw an exception, otherwise just do nothing.
        Address[] recipients = message.getAllRecipients();
        InternetAddress to = (InternetAddress)recipients[0];

        if(to.getAddress().contains("throw@com")) {
            throw new MessagingException();
        }
    }
}
