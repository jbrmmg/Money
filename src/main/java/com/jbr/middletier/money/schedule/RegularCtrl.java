package com.jbr.middletier.money.schedule;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.RegularRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Component
public class RegularCtrl {
    private final RegularRepository regularRepository;

    private final TransactionRepository tranasactionRepository;

    private final ApplicationProperties applicationProperties;

    private static final Logger LOG = LoggerFactory.getLogger(RegularCtrl.class);

    private static SimpleDateFormat loggingSDF = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    public RegularCtrl(RegularRepository regularRepository,
                       TransactionRepository tranasactionRepository,
                       ApplicationProperties applicationProperties ) {
        this.regularRepository = regularRepository;
        this.tranasactionRepository = tranasactionRepository;
        this.applicationProperties = applicationProperties;
    }

    private Date adjustDate(Date transactionDate, String adjustment) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(transactionDate);

        int adjustmentAmt = 0;
        switch(adjustment) {
            case "FW":
                adjustmentAmt = 1;
                break;
            case "BW":
                adjustmentAmt = -1;
                break;
        }

        if(adjustmentAmt != 0) {
            while( (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) || (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) ) {
                calendar.add(Calendar.DATE,adjustmentAmt);
            }

            LOG.info("Date has been adjusted " + adjustment + " " + loggingSDF.format(calendar.getTime()));
        }

        return calendar.getTime();
    }

    private void processRegular(Date today, Regular nextRegular) {
        try {
            // If the next date is today, then create a transaction.
            if (nextRegular.isNextDateToday(today)) {

                Date saveDate = nextRegular.getNextDate(today);
                Date transactionDate = adjustDate(saveDate,nextRegular.getWeekendAdj());

                LOG.info("Create new transaction");
                Transaction regularPayment = new Transaction(nextRegular.getAccount(), nextRegular.getCategory(), transactionDate, nextRegular.getAmount(), nextRegular.getDescription());
                tranasactionRepository.save(regularPayment);

                // Update the regular payment.
                nextRegular.setLastDate(saveDate);
                regularRepository.save(nextRegular);
            }
        } catch( Regular.CannotDetermineNextDateException ex) {
            LOG.error("Cannot determine the next payemnt." + ex.getMessage());
        } catch ( Exception ex) {
            LOG.error("Failed to process regular payment.",ex);
        }
    }

    public void generateRegularPayments(Date forDate) {
        // Generate for date.
        LOG.info("Generate as of: " + loggingSDF.format(forDate));

        // Process the regular payments.
        Iterable<Regular> regularPayments = regularRepository.findAll();

        // Go through each payment.
        for(Regular nextRegular : regularPayments) {
            LOG.info("Process next regular payment " + nextRegular.getId() + " " + nextRegular.getAccount() + " " + nextRegular.getCategory() + " " + nextRegular.getAmount());
            processRegular(forDate, nextRegular);
        }
    }

    @Scheduled(cron = "#{@applicationProperties.regularSchedule}")
    public void generateRegularPayments() {
        if(!applicationProperties.getRegularEnabled()) {
            LOG.info("Skipping regular payments.");
            return;
        }

        // Generate for today.
        Date today = new Date();

        LOG.info("TODAY: " + loggingSDF.format(today));
        generateRegularPayments(new Date());
    }
}
