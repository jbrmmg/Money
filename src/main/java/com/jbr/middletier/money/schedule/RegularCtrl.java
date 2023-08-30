package com.jbr.middletier.money.schedule;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.Regular;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.RegularRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class RegularCtrl {
    private final RegularRepository regularRepository;

    private final TransactionRepository tranasactionRepository;

    private final ApplicationProperties applicationProperties;

    private static final Logger LOG = LoggerFactory.getLogger(RegularCtrl.class);

    @Autowired
    public RegularCtrl(RegularRepository regularRepository,
                       TransactionRepository tranasactionRepository,
                       ApplicationProperties applicationProperties ) {
        this.regularRepository = regularRepository;
        this.tranasactionRepository = tranasactionRepository;
        this.applicationProperties = applicationProperties;
    }

    private LocalDate adjustDate(LocalDate transactionDate, AdjustmentType adjustment) {
        int adjustmentAmt = switch (adjustment) {
            case AT_FORWARD -> 1;
            case AT_BACKWARD -> -1;
            default -> 0;
        };

        if(adjustmentAmt != 0) {
            while( (transactionDate.getDayOfWeek() == DayOfWeek.SUNDAY) || (transactionDate.getDayOfWeek() == DayOfWeek.SATURDAY) ) {
                transactionDate = transactionDate.plusDays(adjustmentAmt);
            }

            LOG.info("Date has been adjusted {} {}", adjustment, transactionDate);
        }

        return transactionDate;
    }

    private void processRegular(LocalDate today, Regular nextRegular) {
        try {
            // If the next date is today, then create a transaction.
            if (nextRegular.isNextDateToday(today)) {

                LocalDate saveDate = nextRegular.getNextDate(today);
                LocalDate transactionDate = adjustDate(saveDate,nextRegular.getWeekendAdj());

                LOG.info("Create new transaction");
                Transaction regularPayment = new Transaction(nextRegular.getAccount(), nextRegular.getCategory(), transactionDate, nextRegular.getAmount(), nextRegular.getDescription());
                tranasactionRepository.save(regularPayment);

                // Update the regular payment.
                nextRegular.setLastDate(saveDate);
                regularRepository.save(nextRegular);
            }
        } catch( CannotDetermineNextDateException ex) {
            LOG.error("Cannot determine the next payemnt.", ex);
        } catch ( Exception ex) {
            LOG.error("Failed to process regular payment.", ex);
        }
    }

    public void generateRegularPayments(LocalDate forDate) {
        // Generate for date.
        LOG.info("Generate as of: {}", forDate);

        // Process the regular payments.
        Iterable<Regular> regularPayments = regularRepository.findAll();

        // Go through each payment.
        for(Regular nextRegular : regularPayments) {
            LOG.info("Process next regular payment {} {} {} {}", nextRegular.getId(), nextRegular.getAccount().getId(), nextRegular.getCategory().getId(), nextRegular.getAmount());
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
        LocalDate today = LocalDate.now();

        LOG.info("TODAY: {}", today);
        generateRegularPayments(today);
    }
}
