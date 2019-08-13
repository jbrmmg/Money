package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.MatchData;
import com.jbr.middletier.money.data.NewTransaction;
import com.jbr.middletier.money.data.ReconcileTransaction;
import com.jbr.middletier.money.data.StatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/jbr")
public class AutoReconciliationDataController {
    final static private Logger LOG = LoggerFactory.getLogger(AutoReconciliationDataController.class);

    private final
    MatchController matchController;

    private final
    AddTransactionController addTransactionController;

    private final
    ReconciliationController reconcileController;

    @Autowired
    public AutoReconciliationDataController(MatchController matchController,
                                            AddTransactionController addTransactionController,
                                            ReconciliationController reconcileController) {
        this.matchController = matchController;
        this.addTransactionController = addTransactionController;
        this.reconcileController = reconcileController;
    }

    private boolean autoReconcileData() {
        // Get the match data an automatically perform the roll forward action (create or reconcile)
        List<MatchData> matchData = matchController.matchFromLastData();

        if(matchData == null)
        {
            LOG.info("Null match data, doing nothing");
            return false;
        }
        
        // Process the data.
        for (MatchData next : matchData ) {
            try {
                // Process the action.
                if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.CREATE.toString())) {
                    // Create the transaction.
                    addTransactionController.addTransactionExt(new NewTransaction(next));
                } else if (next.getForwardAction().equalsIgnoreCase(MatchData.ForwardActionType.RECONCILE.toString())) {
                    // Reconcile the transaction
                    ReconcileTransaction reconcileRequest = new ReconcileTransaction();
                    reconcileRequest.setId(next.getTransactionId());
                    reconcileRequest.setReconcile(true);
                    reconcileController.reconcileExt(reconcileRequest);
                }
            } catch (Exception ex)
            {
                LOG.error("Failed to process match data.",ex);
                return false;
            }
        }

        return true;
    }

    @RequestMapping(path="/ext/money/reconciliation/auto", method= RequestMethod.POST)
    public @ResponseBody StatusResponse reconcileDataExt() {
        LOG.info("Auto Reconcilation Data (ext) ");
        if(autoReconcileData()) {
            return new StatusResponse();
        } else {
            return new StatusResponse("Failed to process auto accept request.");
        }
    }

    @RequestMapping(path="/int/money/reconciliation/auto", method= RequestMethod.POST)
    public @ResponseBody StatusResponse reconcileDataInt() {
        LOG.info("Auto Reconcilation Data ");
        if(autoReconcileData()) {
            return new StatusResponse();
        } else {
            return new StatusResponse("Failed to process auto accept request.");
        }
    }
}
