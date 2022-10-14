package com.jbr.middletier.money.reporting;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.AccountRepository;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.util.TransportWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class EmailGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(EmailGenerator.class);

    private final TransactionRepository transactionRepository;
    private final StatementRepository statementRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final ResourceLoader resourceLoader;
    private final TransportWrapper transportWrapper;

    private void appendRow(StringBuilder sb, String date, String category, String account, String description, Double amount) {
        sb.append("<tr>\n");
        sb.append("<td class=\"date\">").append(date).append("</td>\n");
        sb.append("<td class=\"description\">").append(category).append("</td>\n");
        sb.append("<td class=\"description\">").append(account).append("</td>\n");
        sb.append("<td class=\"description\">").append(description).append("</td>\n");
        if(amount == null) {
            sb.append("<td class=\"amount amount-data\"></td>\n");
        } else  if(amount < 0) {
            sb.append("<td class=\"amount amount-data db\">").append(String.format("%02.2f", amount)).append("</td>\n");
        } else {
            sb.append("<td class=\"amount amount-data\">").append(String.format("%02.2f", amount)).append("</td>\n");
        }
        sb.append("</tr>\n");
    }

    @Autowired
    public EmailGenerator(TransactionRepository transactionRepository,
                          StatementRepository statementRepository,
                          CategoryRepository categoryRepository,
                          AccountRepository accountRepository,
                          ResourceLoader resourceLoader,
                          TransportWrapper transportWrapper) {
        this.transactionRepository = transactionRepository;
        this.statementRepository = statementRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.resourceLoader = resourceLoader;
        this.transportWrapper = transportWrapper;
    }

    public void generateReport( String to,
                                String from,
                                String username,
                                String host,
                                String password,
                                int weeks) throws Exception {
        Iterable<Category> categories = categoryRepository.findAll();

        List<EmailTransaction> emailData = new ArrayList<>();

        // Get the data that we will contain in the email.
        double startAmount = 0.0;
        double endAmount = 0.0;
        double transactionTotal1 = 0;
        double transactionTotal2 = 0;

        LocalDate sevenWeeks = LocalDate.now();
        sevenWeeks = sevenWeeks.plusWeeks(-7);

        // Get the latest statement that is locked for each account.
        Iterable<Account> accounts = accountRepository.findAll();

        for(Account nextAccount : accounts) {
            // Get the latest statement.
            List<Statement> latestStatements = statementRepository.findByIdAccountAndLocked(nextAccount,false);
            for(Statement nextStatement: latestStatements) {
                endAmount += nextStatement.getOpenBalance().getValue();
                startAmount += nextStatement.getOpenBalance().getValue();

                // Get the transactions for this.
                List<Transaction> transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                        nextAccount,
                        nextStatement.getId().getYear(),
                        nextStatement.getId().getMonth());
                for(Transaction nextTransaction : transactions) {
                    endAmount += nextTransaction.getAmount().getValue();
                    transactionTotal1 += nextTransaction.getAmount().getValue();

                    emailData.add(new EmailTransaction(nextTransaction,categories));
                }

                transactions = transactionRepository.findByAccountAndStatementIdYearAndStatementIdMonth(
                        nextAccount,
                        StatementId.getPreviousId(nextStatement.getId()).getYear(),
                        StatementId.getPreviousId(nextStatement.getId()).getMonth());
                for(Transaction nextTransaction : transactions) {
                    if(nextTransaction.getDate().isAfter(sevenWeeks)) {
                        transactionTotal2 += nextTransaction.getAmount().getValue();

                        emailData.add(new EmailTransaction(nextTransaction, categories));
                    }
                }
            }
        }

        emailData.sort((emailTransaction, t1) -> {
            if(emailTransaction.getDate().isBefore(t1.getDate())) {
                return +1;
            } else if (emailTransaction.getDate().isAfter(t1.getDate())) {
                return -1;
            }

            if(emailTransaction.getAmount() > t1.getAmount()) {
                return +1;
            } else if(emailTransaction.getAmount() < t1.getAmount()) {
                return -1;
            }

            return 0;
        });

        startAmount = endAmount;
        startAmount -= transactionTotal1;
        startAmount -= transactionTotal2;

        for(EmailTransaction nextTransaction: emailData) {
            LOG.info("{}", nextTransaction);
        }

        LOG.info(String.format("%02.2f",startAmount));
        LOG.info(String.format("%02.2f",endAmount));
        LOG.info(String.format("%02.2f",transactionTotal1 + transactionTotal2));

        Properties properties = new Properties();
        properties.put("mail.smtp.auth","true");
        properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.host",host);
        properties.put("mail.smtp.port","25");

        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username,password);
                    }
                });

        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject("Credit card bills");

        // Get the email template.
        Resource resource = resourceLoader.getResource("classpath:html/email.html");
        InputStream is = resource.getInputStream();

        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);

        String template = reader.lines().collect(Collectors.joining(System.lineSeparator()));

        StringBuilder sb = new StringBuilder();

        appendRow(sb,"", "", "", "Current Balance", endAmount);
        appendRow(sb,"", "", "", "", null);
        for(EmailTransaction nextTransaction: emailData) {
            appendRow(sb,
                    DateTimeFormatter.ofPattern("dd/MMM").format(nextTransaction.getDate()),
                    nextTransaction.getCategory(),
                    nextTransaction.getAccount(),
                    nextTransaction.toString(),
                    nextTransaction.getAmount());
            LOG.info("{}", nextTransaction);
        }
        appendRow(sb,"", "", "", "", null);
        appendRow(sb,"", "", "", "Bought forward", startAmount);

        message.setContent(template.replace("<!-- TABLEROWS -->", sb.toString()),"text/html");

        transportWrapper.setEmail(message);

        LOG.info("email sent.");
    }
}
