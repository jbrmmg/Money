package com.jbr.middletier.money.schedule;

import com.jbr.middletier.money.config.ApplicationProperties;
import com.jbr.middletier.money.data.primary.Regular;
import com.jbr.middletier.money.data.primary.Transaction;
import com.jbr.middletier.money.data.primary.repository.RegularRepository;
import com.jbr.middletier.money.data.primary.repository.TransactionRepository;
import com.jbr.middletier.money.exceptions.CannotDetermineNextDateException;
import com.jbr.middletier.money.util.DateAdjustmentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class RegularCtrl {
    private final RegularRepository regularRepository;

    private final TransactionRepository transactionRepository;

    private final ApplicationProperties applicationProperties;

    private static final Logger LOG = LoggerFactory.getLogger(RegularCtrl.class);

    @Autowired
    public RegularCtrl(RegularRepository regularRepository,
                       TransactionRepository transactionRepository,
                       ApplicationProperties applicationProperties ) {
        this.regularRepository = regularRepository;
        this.transactionRepository = transactionRepository;
        this.applicationProperties = applicationProperties;
    }

    private LocalDate adjustDate(LocalDate transactionDate, AdjustmentType adjustment) {
        LocalDate adjusted = DateAdjustmentUtil.adjustForWeekend(transactionDate, adjustment);
        if (!adjusted.equals(transactionDate)) {
            LOG.info("Date has been adjusted {} {}", adjustment, adjusted);
        }
        return adjusted;
    }

    private void processRegular(LocalDate today, Regular nextRegular) {
        try {
            // If the next date is today, then create a transaction.
            if (nextRegular.isNextDateToday(today)) {

                LocalDate saveDate = nextRegular.getNextDate(today);
                LocalDate transactionDate = adjustDate(saveDate,nextRegular.getWeekendAdj());

                LOG.info("Create new transaction");
                Transaction regularPayment = new Transaction(nextRegular.getAccount(), nextRegular.getCategory(), transactionDate, nextRegular.getAmount(), nextRegular.getDescription());
                transactionRepository.save(regularPayment);

                // Update the regular payment.
                nextRegular.setLastDate(saveDate);
                regularRepository.save(nextRegular);
            }
        } catch( CannotDetermineNextDateException ex) {
            LOG.error("Cannot determine the next payment.", ex);
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
        LocalDate today = applicationProperties.getToday();

        LOG.info("TODAY: {}", today);
        generateRegularPayments(today);
    }
}
