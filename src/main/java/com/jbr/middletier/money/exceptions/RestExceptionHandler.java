package com.jbr.middletier.money.exceptions;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(AccountAlreadyExistsException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(AccountAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Account already exists", ex));
    }

    @ExceptionHandler(InvalidAccountIdException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(InvalidAccountIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Account id invalid", ex));
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError,apiError.getStatus());
    }
}
