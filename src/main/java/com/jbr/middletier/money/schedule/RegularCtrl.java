package com.jbr.middletier.money.schedule;

import com.jbr.middletier.money.data.NewTransaction;
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
    private final
    RegularRepository regularRepository;

    private final
    TransactionRepository tranasactionRepository;

    final static private Logger LOG = LoggerFactory.getLogger(RegularCtrl.class);

    private static SimpleDateFormat loggingSDF = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    public RegularCtrl(RegularRepository regularRepository,
                       TransactionRepository tranasactionRepository) {
        this.regularRepository = regularRepository;
        this.tranasactionRepository = tranasactionRepository;
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
                SimpleDateFormat sdfNewTran = new SimpleDateFormat(Transaction.TransactionDateFormat);

                Date saveDate = nextRegular.getNextDate(today);
                Date transactionDate = adjustDate(saveDate,nextRegular.getWeekendAdj());

                LOG.info("Create new transaction");
                NewTransaction newTransaction = new NewTransaction( nextRegular.getAccount(), nextRegular.getCategory(), sdfNewTran.format(transactionDate), nextRegular.getAmount() );

                Transaction regularPayment = new Transaction(newTransaction);
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

    @Scheduled(cron = "${middle.tier.regular.schedule:0 30 2 * * ?}")
    public void generateRegularPayments() {
        // Generate for today.
        Date today = new Date();

        LOG.info("TODAY: " + loggingSDF.format(today));
        generateRegularPayments(new Date());
    }
}
