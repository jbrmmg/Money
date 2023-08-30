package com.jbr.middletier.money.exceptions;

import org.springframework.http.HttpStatus;

public class CannotDetermineNextDateException extends MoneyException {
    public CannotDetermineNextDateException(String message)  {
        super(HttpStatus.CONFLICT, message);
    }
}
