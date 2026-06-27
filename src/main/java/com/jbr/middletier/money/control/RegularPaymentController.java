package com.jbr.middletier.money.control;

import com.jbr.middletier.money.dto.RegularDTO;
import com.jbr.middletier.money.exceptions.InvalidRegularIdException;
import com.jbr.middletier.money.exceptions.RegularAlreadyExistsException;
import com.jbr.middletier.money.manager.RegularPaymentManager;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Regular Payments", description = "Recurring scheduled payment configuration")
public class RegularPaymentController {
    private static final Logger LOG = LoggerFactory.getLogger(RegularPaymentController.class);

    private final RegularPaymentManager regularPaymentManager;

    @Autowired
    public RegularPaymentController(RegularPaymentManager regularPaymentManager) {
        this.regularPaymentManager = regularPaymentManager;
    }

    @GetMapping(path="/transaction/regulars")
    public Iterable<RegularDTO> getRegularPayments() {
        LOG.info("Get the regular payments.");
        return this.regularPaymentManager.getRegularPayments();
    }

    @PostMapping(path="/transaction/regulars")
    public Iterable<RegularDTO> createRegularPayment(@Valid @RequestBody RegularDTO regular) throws RegularAlreadyExistsException {
        LOG.info("Create a regular payment");
        this.regularPaymentManager.createRegularPayment(regular);
        return this.regularPaymentManager.getRegularPayments();
    }

    @PutMapping(path="/transaction/regulars")
    public Iterable<RegularDTO> updateRegularPayment(@Valid @RequestBody RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Update a regular payment");
        this.regularPaymentManager.updateRegularPayment(regular);
        return this.regularPaymentManager.getRegularPayments();
    }

    @DeleteMapping(path="/transaction/regulars")
    public Iterable<RegularDTO> deleteRegularPayment(@Valid @RequestBody RegularDTO regular) throws InvalidRegularIdException {
        LOG.info("Delete a regular payment.");
        this.regularPaymentManager.deleteRegularPayment(regular);
        return this.regularPaymentManager.getRegularPayments();
    }
}
