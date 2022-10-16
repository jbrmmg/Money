package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.dto.TransactionDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.*;

@Controller
public class EmailGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(EmailGenerator.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final AccountRepository accountRepository;
    private final TransportWrapper transportWrapper;
    private final ModelMapper modelMapper;

    @Autowired
    public EmailGenerator(TransactionRepository transactionRepository,
                          StatementRepository statementRepository,
                          AccountRepository accountRepository,
                          TransportWrapper transportWrapper,
                          ModelMapper modelMapper) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.transportWrapper = transportWrapper;
        this.modelMapper = modelMapper;
    }

    public void generateReport( String to,
                                String from,
                                String username,
                                String host,
                                String password,
                                long weeks) throws EmailGenerationException {
        try {
            List<TransactionDTO> emailTransactions = new ArrayList<>();

            // Get the data that we will contain in the email.
            double startAmount = 0.0;
            double endAmount = 0.0;
            double transactionTotal1 = 0;
            double transactionTotal2 = 0;

            LocalDate oldest = LocalDate.now();
            oldest = oldest.plusWeeks(-1 * weeks);

            // Get the latest statement that is locked for each account.
            Iterable<Account> accounts = accountRepository.findAll();

            for (Account nextAccount : accounts) {
                // Get the latest statement.
                List<Statement> latestStatements = statementRepository.findByIdAccountAndLocked(nextAccount, false);
                for (Statement nextStatement : latestStatements) {
                    endAmount += nextStatement.getOpenBalance().getValue();
                    startAmount += nextStatement.getOpenBalance().getValue();

                    // Get the transactions for this.
                    List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                            nextAccount,
                            nextStatement.getId().getYear(),
                            nextStatement.getId().getMonth());
                    for (Transaction nextTransaction : transactions) {
                        endAmount += nextTransaction.getAmount().getValue();
                        transactionTotal1 += nextTransaction.getAmount().getValue();

                        emailTransactions.add(modelMapper.map(nextTransaction,TransactionDTO.class));
                    }

                    transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                            nextAccount,
                            StatementId.getPreviousId(nextStatement.getId()).getYear(),
                            StatementId.getPreviousId(nextStatement.getId()).getMonth());
                    for (Transaction nextTransaction : transactions) {
                        if (nextTransaction.getDate().isAfter(oldest)) {
                            transactionTotal2 += nextTransaction.getAmount().getValue();

                            emailTransactions.add(modelMapper.map(nextTransaction,TransactionDTO.class));
                        }
                    }
                }
            }

            emailTransactions.sort((emailTransaction, t1) -> {
                if (emailTransaction.getDate().isBefore(t1.getDate())) {
                    return +1;
                } else if (emailTransaction.getDate().isAfter(t1.getDate())) {
                    return -1;
                }

                if (emailTransaction.getAmount() > t1.getAmount()) {
                    return +1;
                } else if (emailTransaction.getAmount() < t1.getAmount()) {
                    return -1;
                }

                return 0;
            });

            startAmount = endAmount;
            startAmount -= transactionTotal1;
            startAmount -= transactionTotal2;

            for (TransactionDTO nextTransaction : emailTransactions) {
                LOG.info("{}", nextTransaction);
            }

            LOG.info(String.format("%02.2f", startAmount));
            LOG.info(String.format("%02.2f", endAmount));
            LOG.info(String.format("%02.2f", transactionTotal1 + transactionTotal2));

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", "25");

            Session session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Credit card bills");

            // Get the email template.
            EmailHtml html = new EmailHtml(new FinancialAmount(startAmount),emailTransactions);
            message.setContent(html.getHtmlAsString(), "text/html");

            transportWrapper.setEmail(message);

            LOG.info("email sent.");
        } catch (MessagingException e) {
            throw new EmailGenerationException("Failed to send the message",e);
        }
    }
}
