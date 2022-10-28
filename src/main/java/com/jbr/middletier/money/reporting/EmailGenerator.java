package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.config.ApplicationProperties;
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
    private final ApplicationProperties applicationProperties;

    @Autowired
    public EmailGenerator(TransactionRepository transactionRepository,
                          StatementRepository statementRepository,
                          AccountRepository accountRepository,
                          TransportWrapper transportWrapper,
                          ModelMapper modelMapper,
                          ApplicationProperties applicationProperties) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.transportWrapper = transportWrapper;
        this.modelMapper = modelMapper;
        this.applicationProperties = applicationProperties;
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
            FinancialAmount startAmount = new FinancialAmount();
            FinancialAmount endAmount = new FinancialAmount();
            FinancialAmount transactionTotal1 = new FinancialAmount();
            FinancialAmount transactionTotal2 = new FinancialAmount();

            LocalDate oldest = LocalDate.now();
            oldest = oldest.plusWeeks(-1 * weeks);

            // Get the latest statement that is locked for each account.
            Iterable<Account> accounts = accountRepository.findAll();

            for (Account nextAccount : accounts) {
                // Get the latest statement.
                List<Statement> latestStatements = statementRepository.findByIdAccountAndLocked(nextAccount, false);
                for (Statement nextStatement : latestStatements) {
                    endAmount.increment(nextStatement.getOpenBalance());
                    startAmount.increment(nextStatement.getOpenBalance());

                    // Get the transactions for this.
                    List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                            nextAccount,
                            nextStatement.getId().getYear(),
                            nextStatement.getId().getMonth());
                    for (Transaction nextTransaction : transactions) {
                        endAmount.increment(nextTransaction.getAmount());
                        transactionTotal1.increment(nextTransaction.getAmount());

                        emailTransactions.add(modelMapper.map(nextTransaction,TransactionDTO.class));
                    }

                    transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                            nextAccount,
                            StatementId.getPreviousId(nextStatement.getId()).getYear(),
                            StatementId.getPreviousId(nextStatement.getId()).getMonth());
                    for (Transaction nextTransaction : transactions) {
                        if (nextTransaction.getDate().isAfter(oldest)) {
                            transactionTotal2.increment(nextTransaction.getAmount().getValue());

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
            startAmount.decrement(transactionTotal1);
            startAmount.decrement(transactionTotal2);

            for (TransactionDTO nextTransaction : emailTransactions) {
                LOG.info("{}", nextTransaction);
            }

            LOG.info("Start:        {}", startAmount);
            LOG.info("End:          {}", endAmount);
            LOG.info("Transaction 1 {}", transactionTotal1);
            LOG.info("Transaction 2 {}", transactionTotal2);

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", this.applicationProperties.getSmtpPort());

            Session session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject("Credit card bills");

            // Get the email template.
            EmailHtml html = new EmailHtml(startAmount,emailTransactions);
            message.setContent(html.getHtmlAsString(), "text/html");

            if(!host.equals(applicationProperties.getIgnoreEmailHost())) {
                transportWrapper.setEmail(message);
            }

            LOG.info("email sent.");
        } catch (MessagingException e) {
            throw new EmailGenerationException("Failed to send the message",e);
        }
    }
}
