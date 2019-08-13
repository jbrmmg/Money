package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by jason on 12/04/17.
 */
@Controller
@RequestMapping("/jbr")
public class DeleteReconcilationDataController {
    final static private Logger LOG = LoggerFactory.getLogger(DeleteReconcilationDataController.class);

    private final
    ReconciliationRepository reconciliationRepository;

    @Autowired
    public DeleteReconcilationDataController(ReconciliationRepository reconciliationRepository) {
        this.reconciliationRepository = reconciliationRepository;
    }

    private void clearRepositoryData() {
        reconciliationRepository.deleteAll();
    }

    @RequestMapping(path="/ext/money/reconciliation/clear", method= RequestMethod.DELETE)
    public @ResponseBody StatusResponse reconcileDataExt() {
        LOG.info("Clear Reconcilation Data (ext) ");
        clearRepositoryData();

        return new StatusResponse();
    }

    @RequestMapping(path="/int/money/reconciliation/clear", method= RequestMethod.DELETE)
    public @ResponseBody StatusResponse reconcileDataInt() {
        LOG.info("Clear Reconcilation Data ");
        clearRepositoryData();

        return new StatusResponse();
    }
}
