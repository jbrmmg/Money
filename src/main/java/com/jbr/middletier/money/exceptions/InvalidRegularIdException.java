package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.RegularDTO;
import org.springframework.http.HttpStatus;

public class InvalidRegularIdException extends MoneyException {
    public InvalidRegularIdException(RegularDTO regular) {
        super(HttpStatus.CONFLICT, "Cannot find regular payment with id " + regular.getId());
    }
}
