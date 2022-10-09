package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.exceptions.InvalidRegularIdException;
import com.jbr.middletier.money.exceptions.RegularAlreadyExistsException;
import com.jbr.middletier.money.manager.RegularPaymentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/jbr")
public class RegularPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(RegularPaymentController.class);

    private final RegularPaymentManager regularPaymentManager;

    @Autowired
    public RegularPaymentController(RegularPaymentManager regularPaymentManager) {
        this.regularPaymentManager = regularPaymentManager;
    }

    @GetMapping(path="/ext/money/transaction/regulars")
    public @ResponseBody Iterable<RegularDTO> getRegularPaymentsExt() {
        LOG.info("Get the regular payments. (ext)");
        return this.regularPaymentManager.getRegularPayments();
    }

    @GetMapping(path="/int/money/transaction/regulars")
    public @ResponseBody Iterable<RegularDTO> getRegularPaymentsInt() {
        LOG.info("Get the regular payments.(int)");
        return this.regularPaymentManager.getRegularPayments();
    }

    @PostMapping(path="/int/money/transaction/regulars")
    public @ResponseBody Iterable<RegularDTO> getRegularPaymentsCreateInt(@RequestBody RegularDTO regular) throws RegularAlreadyExistsException {
        LOG.info("Create a regular payment");
        this.regularPaymentManager.createRegularPayment(regular);

        return this.regularPaymentManager.getRegularPayments();
    }

    @PutMapping(path="/int/money/transaction/regulars")
    public @ResponseBody Iterable<RegularDTO> getRegularPaymentsUpdateInt(@RequestBody RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Update a regular payment");
        this.regularPaymentManager.updateRegularPayment(regular);

        return this.regularPaymentManager.getRegularPayments();
    }

    @DeleteMapping(path="/int/money/transaction/regulars")
    public @ResponseBody Iterable<RegularDTO> getRegularPaymentsDeleteInt(@RequestBody RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Delete a regular payment.");
        this.regularPaymentManager.deleteRegularPayment(regular);

        return this.regularPaymentManager.getRegularPayments();
    }
}
