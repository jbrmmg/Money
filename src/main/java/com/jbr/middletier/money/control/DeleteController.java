package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

/**
 * Created by jason on 07/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class DeleteController {
    final static private Logger LOG = LoggerFactory.getLogger(DeleteController.class);

    private final
    TransactionRepository transactionRepository;

    @Autowired
    public DeleteController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    private boolean deleteTransaction(int transactionId) {
        LOG.info("Delete transaction.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionId);

        if(transaction.isPresent()) {
            // If the transaction is not reconciled then it can be deleted.
            if(!transaction.get().reconciled()) {
                transactionRepository.deleteById(transactionId);
                return true;
            }
        }

        return false;
    }

    @RequestMapping(path="/ext/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody StatusResponse deleteExternal(@RequestParam(value="transactionId", defaultValue="0") int transactionId) {
        if(deleteTransaction(transactionId)) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to delete transaction.");
    }

    @RequestMapping(path="/int/money/delete", method= RequestMethod.DELETE)
    public @ResponseBody StatusResponse deleteInternal( @RequestParam(value="transactionId", defaultValue="0") int transactionId) {
        if(deleteTransaction(transactionId)) {
            return new StatusResponse();
        }

        return new StatusResponse("Failed to delete transaction.");
    }
}
