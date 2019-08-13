package com.jbr.middletier.money.control;

import com.jbr.middletier.money.data.StatusResponse;
import com.jbr.middletier.money.data.Transaction;
import com.jbr.middletier.money.data.UpdateTransaction;
import com.jbr.middletier.money.dataaccess.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * Created by jason on 08/03/17.
 */
@Controller
@RequestMapping("/jbr")
public class UpdateTransactionController {
    final static private Logger LOG = LoggerFactory.getLogger(UpdateTransactionController.class);

    private final
    TransactionRepository transactionRepository;

    @Autowired
    public UpdateTransactionController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @ExceptionHandler(IllegalStateException.class)
    public void handleIllegalArgumentException(IllegalStateException e, HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value());
    }

    private void updateTransacation(UpdateTransaction transactionRequest) {
        LOG.info("Request transction update.");

        // Get the transaction.
        Optional<Transaction> transaction = transactionRepository.findById(transactionRequest.getId());

        if(transaction.isPresent()) {
            transaction.get().setAmount(transactionRequest.getAmount());

            transactionRepository.save(transaction.get());
            LOG.info("Request transction updated.");

            if(transaction.get().hasOppositeId()) {
                transaction = transactionRepository.findById(transaction.get().getOppositeId());

                if(transaction != null) {
                    transaction.get().setAmount(-1 * transactionRequest.getAmount());
                    transactionRepository.save(transaction.get());
                    LOG.info("Request transction updated (opposite).");
                }
            }
            return;
        }

        throw new IllegalStateException(String.format("Transaction does not exist %d", transactionRequest.getId()));
    }

    @RequestMapping(path="/ext/money/transaction/update", method= RequestMethod.POST)
    public @ResponseBody
    StatusResponse updateTransactionExt(@RequestBody UpdateTransaction transaction) {
        updateTransacation(transaction);
        return new StatusResponse();
    }

    @RequestMapping(path="/int/money/transaction/update", method= RequestMethod.POST)
    public @ResponseBody
    StatusResponse updateTransactionInt(@RequestBody UpdateTransaction transaction) {
        updateTransacation(transaction);
        return new StatusResponse();
    }
}
