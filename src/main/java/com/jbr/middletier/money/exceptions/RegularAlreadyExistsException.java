package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.RegularDTO;

public class RegularAlreadyExistsException extends Exception {
    public RegularAlreadyExistsException(RegularDTO regular) {
        super("Regular Payment already exists " + regular.getId());
    }
}
