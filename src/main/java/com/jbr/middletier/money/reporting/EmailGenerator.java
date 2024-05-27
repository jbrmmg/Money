package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.StatementId;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.dto.EmailRequestDTO;
import com.jbr.middletier.money.exceptions.EmailGenerationException;
import com.jbr.middletier.money.manager.AccountManager;
import com.jbr.middletier.money.manager.AccountTransactionManager;
import com.jbr.middletier.money.manager.StatementManager;
import com.jbr.middletier.money.util.FinancialAmount;
import com.jbr.middletier.money.util.TransportWrapper;
import com.jbr.middletier.money.xml.html.EmailHtml;
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

    private final AccountTransactionManager transactionManager;
    private final StatementManager statementManager;
    private final AccountManager accountManager;
    private final TransportWrapper transportWrapper;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public EmailGenerator(AccountTransactionManager transactionManager,
                          StatementManager statementManager,
                          AccountManager accountManager,
                          TransportWrapper transportWrapper,
                          ApplicationProperties applicationProperties) {
        this.transactionManager = transactionManager;
        this.statementManager = statementManager;
        this.accountManager = accountManager;
        this.transportWrapper = transportWrapper;
        this.applicationProperties = applicationProperties;
    }

    private void getTransactions(List<Transaction> emailTransactions,
                                 FinancialAmount startAmount,
                                 FinancialAmount endAmount,
                                 FinancialAmount transactionTotal1,
                                 FinancialAmount transactionTotal2,
                                 int weeks) {
        LocalDate oldest = applicationProperties.getToday();
        oldest = oldest.plusWeeks(-1L * weeks);

        // Get the latest statement that is locked for each account.
        for (Account nextAccount : accountManager.getAllExternal()) {
            // Get the latest statement.
            List<Statement> latestStatements = statementManager.getLatestStatementInternal(nextAccount);
            for (Statement nextStatement : latestStatements) {
                endAmount.increment(nextStatement.getOpenBalance());
                startAmount.increment(nextStatement.getOpenBalance());

                // Get the transactions for this.
                List<Transaction> transactions = transactionManager.getInternalTransactionsForStatement(nextAccount,nextStatement.getId());
                for (Transaction nextTransaction : transactions) {
                    endAmount.increment(nextTransaction.getAmount());
                    transactionTotal1.increment(nextTransaction.getAmount());

                    emailTransactions.add(nextTransaction);
                }

                transactions = transactionManager.getInternalTransactionsForStatement(nextAccount, StatementId.getPreviousId(nextStatement.getId()));
                for (Transaction nextTransaction : transactions) {
                    if (nextTransaction.getDate().isAfter(oldest)) {
                        transactionTotal2.increment(nextTransaction.getAmount().getValue());

                        emailTransactions.add(nextTransaction);
                    }
                }
            }
        }
    }

    public void generateReport(EmailRequestDTO request) throws EmailGenerationException {
        try {
            List<Transaction> emailTransactions = new ArrayList<>();

            // Get the data that we will contain in the email.
            FinancialAmount startAmount = new FinancialAmount();
            FinancialAmount endAmount = new FinancialAmount();
            FinancialAmount transactionTotal1 = new FinancialAmount();
            FinancialAmount transactionTotal2 = new FinancialAmount();

            getTransactions(emailTransactions,startAmount,endAmount,transactionTotal1,transactionTotal2,request.getWeeks());

            emailTransactions.sort((emailTransaction, t1) -> {
                if (emailTransaction.getDate().isBefore(t1.getDate())) {
                    return +1;
                } else if (emailTransaction.getDate().isAfter(t1.getDate())) {
                    return -1;
                }

                if (emailTransaction.getAmount().isGreaterThan(t1.getAmount())) {
                    return +1;
                } else if (emailTransaction.getAmount().isLessThan(t1.getAmount())) {
                    return -1;
                }

                return 0;
            });

            startAmount = endAmount;
            startAmount.decrement(transactionTotal1);
            startAmount.decrement(transactionTotal2);

            for (Transaction nextTransaction : emailTransactions) {
                LOG.info("{}", nextTransaction);
            }

            LOG.info("Start:        {}", startAmount);
            LOG.info("End:          {}", endAmount);
            LOG.info("Transaction 1 {}", transactionTotal1);
            LOG.info("Transaction 2 {}", transactionTotal2);

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.host", request.getHost());
            properties.put("mail.smtp.port", this.applicationProperties.getSmtpPort());

            Session session = Session.getInstance(properties,
                    new javax.mail.Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(request.getUsername(), request.getPassword());
                        }
                    });

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(request.getFrom()));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));
            message.setSubject("Credit card bills");

            // Get the email template.
            EmailHtml html = new EmailHtml(startAmount,emailTransactions);
            message.setContent(html.getHtmlAsString(), "text/html");

            transportWrapper.sendEmail(message);

            LOG.info("email sent.");
        } catch (MessagingException e) {
            throw new EmailGenerationException("Failed to send the message",e);
        }
    }
}
