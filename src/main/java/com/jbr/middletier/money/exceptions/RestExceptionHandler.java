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
    // TODO make sure all exceptions are handled here & method names.
    @ExceptionHandler(AccountAlreadyExistsException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(AccountAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Account already exists", ex));
    }

    @ExceptionHandler(InvalidAccountIdException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(InvalidAccountIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Account id invalid", ex));
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    protected ResponseEntity<Object> handleCateogryAlreadyExist(CategoryAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Category already exists", ex));
    }

    @ExceptionHandler(DeleteSystemCategoryException.class)
    protected ResponseEntity<Object> handlDeleteSystemCategory(DeleteSystemCategoryException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Cannot delete system category", ex));
    }

    @ExceptionHandler(InvalidCategoryIdException.class)
    protected ResponseEntity<Object> handleInvalidCategoryId(InvalidCategoryIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Category id invalid", ex));
    }

    @ExceptionHandler(InvalidStatementIdException.class)
    protected ResponseEntity<Object> handleInvalidStatementId(InvalidStatementIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Statement id invalid", ex));
    }

    @ExceptionHandler(StatementAlreadyExists.class)
    protected ResponseEntity<Object> handleStatementAlreadyExists(StatementAlreadyExists ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Statement already exists", ex));
    }

    @ExceptionHandler(CannotUpdateSystemCategory.class)
    protected ResponseEntity<Object> handleCannotUpdateSystemCategory(CannotUpdateSystemCategory ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Cannot update system category", ex));
    }

    @ExceptionHandler(StatementAlreadyLockedException.class)
    protected ResponseEntity<Object> handleStatementAlreadyLocked(StatementAlreadyLockedException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT, "Cannot lock a statement again.", ex));
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError,apiError.getStatus());
    }
}
