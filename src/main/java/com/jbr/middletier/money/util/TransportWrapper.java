package com.jbr.middletier.money.util;

import javax.mail.Message;
import javax.mail.MessagingException;

public interface TransportWrapper {
    void sendEmail(Message message) throws MessagingException;
}
