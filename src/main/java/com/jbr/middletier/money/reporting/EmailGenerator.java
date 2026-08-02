package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Account;
import com.jbr.middletier.money.data.primary.Statement;
import com.jbr.middletier.money.data.primary.StatementId;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.dto.EmailRequestDTO;
import com.jbr.middletier.money.dto.ReportDefinitionDTO;
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Controller
public class EmailGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(EmailGenerator.class);

    private static final List<String> MONTH_NAMES = List.of(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );

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

        for (Account nextAccount : accountManager.getAllExternal()) {
            List<Statement> latestStatements = statementManager.getLatestStatementInternal(nextAccount);
            for (Statement nextStatement : latestStatements) {
                endAmount.increment(nextStatement.getOpenBalance());
                startAmount.increment(nextStatement.getOpenBalance());

                List<Transaction> transactions = transactionManager.getInternalTransactionsForStatement(nextAccount, nextStatement.getId());
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

            FinancialAmount startAmount = new FinancialAmount();
            FinancialAmount endAmount = new FinancialAmount();
            FinancialAmount transactionTotal1 = new FinancialAmount();
            FinancialAmount transactionTotal2 = new FinancialAmount();

            getTransactions(emailTransactions, startAmount, endAmount, transactionTotal1, transactionTotal2, request.getWeeks());

            emailTransactions.sort((emailTransaction, t1) -> {
                if (emailTransaction.getDate().isBefore(t1.getDate())) return +1;
                else if (emailTransaction.getDate().isAfter(t1.getDate())) return -1;
                if (emailTransaction.getAmount().isGreaterThan(t1.getAmount())) return +1;
                else if (emailTransaction.getAmount().isLessThan(t1.getAmount())) return -1;
                return 0;
            });

            startAmount = endAmount;
            startAmount.decrement(transactionTotal1);
            startAmount.decrement(transactionTotal2);

            for (Transaction nextTransaction : emailTransactions) {
                LOG.info("{}", nextTransaction);
            }

            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "false");
            properties.put("mail.smtp.host", this.applicationProperties.getSmtpHost());
            properties.put("mail.smtp.port", this.applicationProperties.getSmtpPort());

            Session session = Session.getInstance(properties);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(this.applicationProperties.getSmtpFrom()));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));
            message.setSubject("Credit card bills");

            EmailHtml html = new EmailHtml(startAmount, emailTransactions);
            message.setContent(html.getHtmlAsString(), "text/html");

            transportWrapper.sendEmail(message);
            LOG.info("email sent.");
        } catch (MessagingException e) {
            throw new EmailGenerationException("Failed to send the message", e);
        }
    }

    public List<ReportDefinitionDTO> getAvailableReports() {
        List<ReportDefinitionDTO> reports = new ArrayList<>();
        File[] dirs = new File(applicationProperties.getReportShare()).listFiles(File::isDirectory);
        if (dirs == null) return reports;

        for (File dir : dirs) {
            try {
                int year = Integer.parseInt(dir.getName());
                if (new File(dir, "annual.html").exists()) {
                    reports.add(new ReportDefinitionDTO("annual", year, null));
                }
                for (int m = 1; m <= 12; m++) {
                    if (new File(dir, MONTH_NAMES.get(m - 1) + ".html").exists()) {
                        reports.add(new ReportDefinitionDTO("monthly", year, m));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        reports.sort(Comparator.comparingInt(ReportDefinitionDTO::getYear)
                .thenComparingInt(r -> r.getMonth() != null ? r.getMonth() : 0));
        return reports;
    }

    public void sendReport(ReportDefinitionDTO definition) throws EmailGenerationException {
        try {
            String filePath;
            String subject;
            if ("annual".equals(definition.getType())) {
                filePath = applicationProperties.getReportShare() + "/" + definition.getYear() + "/annual.html";
                subject = "Annual Report - " + definition.getYear();
            } else {
                String monthName = MONTH_NAMES.get(definition.getMonth() - 1);
                filePath = applicationProperties.getReportShare() + "/" + definition.getYear() + "/" + monthName + ".html";
                subject = "Monthly Report - " + monthName + " " + definition.getYear();
            }
            String recipient = (definition.getTo() != null && !definition.getTo().isBlank())
                    ? definition.getTo()
                    : applicationProperties.getSmtpTo();
            sendHtmlEmail(subject, Files.readString(Paths.get(filePath)), recipient);
        } catch (IOException e) {
            throw new EmailGenerationException("Failed to read report file: " + definition.getYear(), e);
        }
    }

    public void sendNotification(String subject, String text) throws EmailGenerationException {
        sendHtmlEmail(subject, "<html><body><p>" + text + "</p></body></html>", applicationProperties.getSmtpTo());
    }

    private void sendHtmlEmail(String subject, String html, String recipient) throws EmailGenerationException {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "false");
            properties.put("mail.smtp.host", applicationProperties.getSmtpHost());
            properties.put("mail.smtp.port", applicationProperties.getSmtpPort());

            Session session = Session.getInstance(properties);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(applicationProperties.getSmtpFrom()));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=UTF-8");

            transportWrapper.sendEmail(message);
            LOG.info("Email sent: {} -> {}", subject, recipient);
        } catch (MessagingException e) {
            throw new EmailGenerationException("Failed to send email: " + subject, e);
        }
    }
}
