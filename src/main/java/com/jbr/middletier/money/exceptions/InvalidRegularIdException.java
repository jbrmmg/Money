package com.jbr.middletier.money.exceptions;

import com.jbr.middletier.money.dto.RegularDTO;

public class InvalidRegularIdException extends Exception {
    public InvalidRegularIdException(RegularDTO regular) {
        super("Cannot find regular payment with id " + regular.getId());
    }
}
