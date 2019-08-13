package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.*;
import com.jbr.middletier.money.dataaccess.CategoryRepository;
import com.jbr.middletier.money.dataaccess.ReconciliationRepository;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Created by jason on 15/04/17.
 */

@Controller
@RequestMapping("/jbr")
public class ReconciliationCategoryController {
    final static private Logger LOG = LoggerFactory.getLogger(ReconciliationCategoryController.class);

    private final ReconciliationRepository reconciliationRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public ReconciliationCategoryController(ReconciliationRepository reconciliationRepository,
                                            CategoryRepository categoryRepository,
                                            TransactionRepository transactionRepository) {
        this.reconciliationRepository = reconciliationRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    private void transactionCategoryUpdate(ReconcileUpdate reconciliationUpdate) {
        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(reconciliationUpdate.getId());

        if(transaction.isPresent()) {
            // Is the category valid?
            Optional<Category> category =  categoryRepository.findById(reconciliationUpdate.getCategoryId());

            if(category.isPresent()) {
                if(!transaction.get().hasOppositeId()) {
                    LOG.info("Category updated for - " + Integer.toString(reconciliationUpdate.getId()));
                    transaction.get().setCategory(reconciliationUpdate.getCategoryId());
                    transactionRepository.save(transaction.get());
                }
            } else {
                LOG.info("Invalid category.");
            }

        } else {
            LOG.info("Invalid id (transaction).");
        }
    }

    private void reconciliationCategoryUpdate(ReconcileUpdate reconciliationUpdate) {
        // Get the reconciliation data.
        Optional<ReconciliationData> reconciliationData = reconciliationRepository.findById(reconciliationUpdate.getId());

        if(reconciliationData.isPresent()) {
            // Is the category valid?
            Optional<Category> category =  categoryRepository.findById(reconciliationUpdate.getCategoryId());

            if(category.isPresent()) {
                LOG.info("Category updated for - " + Integer.toString(reconciliationUpdate.getId()));
                reconciliationData.get().setCategory(category.get().getId(), category.get().getColour());
                reconciliationRepository.save(reconciliationData.get());
            } else {
                LOG.info("Invalid category.");
            }
        } else {
            LOG.info("Invalid id.");
        }
    }

    private StatusResponse processReconcileUpdate(ReconcileUpdate reconciliationUpdate) {
        LOG.info("Update category (ext) - " + Integer.toString(reconciliationUpdate.getId()) + " - " + reconciliationUpdate.getCategoryId() + " - " + reconciliationUpdate.getType());

        if(reconciliationUpdate.getType().equalsIgnoreCase("trn")) {
            transactionCategoryUpdate(reconciliationUpdate);
            return new StatusResponse();
        }

        reconciliationCategoryUpdate(reconciliationUpdate);
        return new StatusResponse();
    }

    @RequestMapping(path="/ext/money/reconciliation/update", method= RequestMethod.POST)
    public @ResponseBody StatusResponse reconcileCategoryExt(@RequestBody ReconcileUpdate reconciliationUpdate ) {
        return processReconcileUpdate(reconciliationUpdate);
    }

    @RequestMapping(path="/int/money/reconciliation/update", method= RequestMethod.POST)
    public @ResponseBody StatusResponse reconcileCategoryInt(@RequestBody ReconcileUpdate reconciliationUpdate) {
        return processReconcileUpdate(reconciliationUpdate);
    }
}
