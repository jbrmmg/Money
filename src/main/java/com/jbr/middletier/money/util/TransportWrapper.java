package com.jbr.middletier.money.util;

import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

@Component
public class TransportWrapper {
    // Wrapper to help with unit testing.
    public void setEmail(Message message) throws MessagingException {
        Transport.send(message);
    }
}
