package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.RegularDTO;
import org.springframework.http.HttpStatus;

public class RegularAlreadyExistsException extends MoneyException {
    public RegularAlreadyExistsException(RegularDTO regular) {
        super(HttpStatus.CONFLICT, "Regular Payment already exists " + regular.getId());
    }
}
