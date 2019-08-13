package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.AllTransaction;
import com.jbr.middletier.money.data.MatchData;
import com.jbr.middletier.money.data.ReconciliationData;
import com.jbr.middletier.money.data.Statement;
import com.jbr.middletier.money.dataaccess.AllTransactionRepository;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import com.jbr.middletier.money.dataaccess.StatementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.jbr.middletier.money.dataaccess.AllTransactionSpecifications.accountIn;
import static com.jbr.middletier.money.dataaccess.AllTransactionSpecifications.notLocked;

/**
 * Created by jason on 11/04/17.
 */
@SuppressWarnings("unchecked")
@Controller
@RequestMapping("/jbr")
public class MatchController {
    final static private Logger LOG = LoggerFactory.getLogger(MatchController.class);

    private final AllTransactionRepository allTransactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final StatementRepository statementRepository;
    private String lastAccount = "UNKN";

    @Autowired
    public MatchController(AllTransactionRepository allTransactionRepository,
                           ReconciliationRepository reconciliationRepository,
                           StatementRepository statementRepository) {
        this.allTransactionRepository = allTransactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.statementRepository = statementRepository;
    }

    private class MatchInformation {
        ReconciliationData recociliationData;
        public AllTransaction transaction;
        long daysAway;

        MatchInformation() {
            this.recociliationData = null;
            this.transaction = null;
            this.daysAway = 0;
        }

        boolean exactMatch() {
            return this.recociliationData != null && this.daysAway == 0;
        }

        boolean closeMatch() {
            return this.recociliationData != null && this.daysAway != 0;
        }
    }

    private void checkReconciliationData(ReconciliationData nextReconciliationData, Iterable<AllTransaction> transactions, Map<Integer,MatchInformation> trnMatches, MatchData matchData, List<ReconciliationData> repeats) {
        // Remember best potential match.
        long bestDaysAway = -1;
        MatchInformation bestTrnMatch = null;

        // Do any existing transactions match? Or close match (amount)
        for(AllTransaction nextTransaction : transactions) {
            Integer transactionId = nextTransaction.getId();
            Integer transactionDate = nextTransaction.getDateDay();

            // Create match information.
            if(!trnMatches.containsKey(nextTransaction.getId())) {
                trnMatches.put(nextTransaction.getId(),new MatchInformation());
            }
            MatchInformation trnMatch = trnMatches.get(nextTransaction.getId());
            trnMatch.transaction = nextTransaction;

            // Is this transaction already exactly matched?
            if(trnMatch.exactMatch()) {
                continue;
            }

            // Is this an exact match?
            //noinspection EqualsBetweenInconvertibleTypes
            if(nextReconciliationData.equals(nextTransaction)) {
                trnMatch.recociliationData = nextReconciliationData;
                trnMatch.daysAway = 0;
                matchData.matchTransaction(nextTransaction);
                return;
            }

            // Does the amount match?
            long difference = nextReconciliationData.closeMatch(nextTransaction);
            if((difference == -1) || (difference >= 4)) {
                continue;
            }

            // Set details of the match.
            if(!trnMatch.closeMatch() || trnMatch.daysAway > difference) {
                if( ( bestDaysAway == -1 )  || (difference < bestDaysAway) ) {
                    bestDaysAway = difference;
                    bestTrnMatch = trnMatch;
                }
            }
        }

        // If there was a good match, the use it.
        if(bestTrnMatch != null) {
            // If the transaction was already matched, then need to repeat the search.
            if (bestTrnMatch.recociliationData != null) {
                repeats.add(bestTrnMatch.recociliationData);
            }

            bestTrnMatch.daysAway = bestDaysAway;
            bestTrnMatch.recociliationData = nextReconciliationData;
        }
    }

    public List<MatchData> matchFromLastData()
    {
        if (lastAccount.equalsIgnoreCase("UNKN") )
            return null;

        return matchData(lastAccount);
    }

    private List<MatchData> matchData(String account) {
        // Attempt to match the reconciliation with the data in the account specified.

        // Get all transactions that are 'unlocked' on the account.
        Iterable<AllTransaction> transactions = allTransactionRepository.findAll(Specification.where(notLocked()).and(accountIn(new String[] {account})), new Sort(Sort.Direction.ASC,"date", "amount"));

        // Get all the reconciliation data.
        List<ReconciliationData> reconciliationData = reconciliationRepository.findAllByOrderByDateAsc();

        // Log the data.
        if(LOG.isDebugEnabled()) {
            for (ReconciliationData nextReconciliationData : reconciliationData) {
                StringBuilder logData = new StringBuilder();

                logData.append("Reconcile Data - ");

                logData.append(nextReconciliationData.getId());
                logData.append(" ");

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                logData.append(sdf.format(nextReconciliationData.getDate()));
                logData.append(" ");

                logData.append(nextReconciliationData.getCategory());
                logData.append(" ");

                DecimalFormat df = new DecimalFormat("#,##0.00");
                logData.append(df.format(nextReconciliationData.getAmount()));

                LOG.debug(logData.toString());
            }

            for (AllTransaction nextTransaction : transactions) {
                StringBuilder logData = new StringBuilder();

                logData.append("Transaction - ");

                logData.append(nextTransaction.getId());
                logData.append(" ");

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
                logData.append(sdf.format(nextTransaction.getDate()));
                logData.append(" ");

                logData.append(nextTransaction.getCategory());
                logData.append(" ");

                DecimalFormat df = new DecimalFormat("#,##0.00");
                logData.append(df.format(nextTransaction.getAmount()));

                LOG.debug(logData.toString());
            }
        }

        // Create a map for transactions.
        Map<Integer,MatchInformation> trnMatches = new HashMap<>();

        // Create a match data map.
        Map<Integer,Integer> matchDataMap = new HashMap<>();

        // Create the match data.
        List<MatchData> result = new ArrayList<>();
        List<ReconciliationData> repeats = new ArrayList<>();

        LOG.info("Matching data");
        for(ReconciliationData nextReconciliationData : reconciliationData) {
            // Create a match data for this item.
            MatchData newMatchData = new MatchData(nextReconciliationData, account);
            matchDataMap.put(nextReconciliationData.getId(),result.size());
            result.add(newMatchData);

            // Check this instance.
            checkReconciliationData(nextReconciliationData, transactions, trnMatches, newMatchData, repeats);
        }

        // Repeat if necessary.
        while(repeats.size() > 0) {
            List<ReconciliationData> prevRepeats = repeats;
            repeats = new ArrayList<>();

            for (ReconciliationData nextReconciliationData : prevRepeats) {
                checkReconciliationData(nextReconciliationData, transactions, trnMatches, result.get(matchDataMap.get(nextReconciliationData.getId())), repeats);
            }
        }

        // Update the result with any partial matches.
        LOG.info("Matching partial data");
        for(MatchInformation nextMatchInfo : trnMatches.values()) {
            if(nextMatchInfo.closeMatch()) {
                // Get the match data index.
                int matchDataIndex = matchDataMap.get(nextMatchInfo.recociliationData.getId());

                // Get the Match Data.
                MatchData matchData = result.get(matchDataIndex);

                // Set the close match transaction.
                matchData.closeMatchTransaction(nextMatchInfo.transaction);
            }
        }

        // Add a list of any reconciled transaction that is not matched.
        LOG.info("Add reconciled transactions not matched");
        for (AllTransaction nextTransaction : transactions ) {
            if(nextTransaction.getReconciled()) {
                if(trnMatches.containsKey(nextTransaction.getId())){
                    MatchInformation matchInformation = trnMatches.get(nextTransaction.getId());
                    if(matchInformation.recociliationData == null) {
                        result.add(new MatchData(nextTransaction));
                    }
                } else {
                    result.add(new MatchData(nextTransaction));
                }
            }
        }

        Collections.sort(result);

        // Update the opening balance information.
        List<Statement> unlockedStatement = statementRepository.findByAccountAndLocked(account,"N");
        if(unlockedStatement.size() != 1) {
            LOG.info("Number of statements was not 1.");
        } else {
            double rollingAmount = unlockedStatement.get(0).getOpenBalance();

            // Set the open balance data.
            for(MatchData nextMatchData : result) {
                if(!nextMatchData.getForwardAction().equalsIgnoreCase("UNRECONCILE")) {
                    nextMatchData.setBeforeAmount(rollingAmount);
                    rollingAmount += nextMatchData.getAmount();
                    nextMatchData.setAfterAmount(rollingAmount);
                }
            }
        }

        return result;
    }

    @RequestMapping(path="/ext/money/match", method= RequestMethod.GET)
    public @ResponseBody
    List<MatchData> matchExt(@RequestParam(value="account", defaultValue="UNKN") String account) {
        LOG.info("External match data - reconciliation data with reconciled transactions");
        lastAccount = account;
        return matchData(account);
    }

    @RequestMapping(path="/int/money/match", method= RequestMethod.GET)
    public @ResponseBody
    List<MatchData>  matchInt(@RequestParam(value="account", defaultValue="UNKN") String account) {
        LOG.info("Internal match data - reconciliation data with reconciled transactions");
        lastAccount = account;
        return matchData(account);
    }
}
