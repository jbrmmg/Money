package com.jbr.middletier.money.exceptions;

import com.itextpdf.text.DocumentException;
import org.apache.batik.transcoder.TranscoderException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.FileNotFoundException;
import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(AccountAlreadyExistsException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(AccountAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Account already exists", ex));
    }

    @ExceptionHandler(InvalidAccountIdException.class)
    protected ResponseEntity<Object> handleBackupAlreadyExist(InvalidAccountIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Account id invalid", ex));
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
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Category id invalid", ex));
    }

    @ExceptionHandler(InvalidStatementIdException.class)
    protected ResponseEntity<Object> handleInvalidStatementId(InvalidStatementIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"Statement id invalid", ex));
    }

    @ExceptionHandler(StatementAlreadyExists.class)
    protected ResponseEntity<Object> handleStatementAlreadyExists(StatementAlreadyExists ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Statement already exists", ex));
    }

    @ExceptionHandler(CannotUpdateSystemCategory.class)
    protected ResponseEntity<Object> handleCannotUpdateSystemCategory(CannotUpdateSystemCategory ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FORBIDDEN,"Cannot update system category", ex));
    }

    @ExceptionHandler(StatementAlreadyLockedException.class)
    protected ResponseEntity<Object> handleStatementAlreadyLocked(StatementAlreadyLockedException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FORBIDDEN, "Cannot lock a statement again.", ex));
    }

    @ExceptionHandler(CannotDeleteLockedStatement.class)
    protected ResponseEntity<Object> handleStatementAlreadyLocked(CannotDeleteLockedStatement ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FORBIDDEN, "Cannot delete locked statement.", ex));
    }

    @ExceptionHandler(CannotDeleteLastStatement.class)
    protected ResponseEntity<Object> handleLastStatement(CannotDeleteLastStatement ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FORBIDDEN, "Cannot delete last statement.", ex));
    }

    @ExceptionHandler(RegularAlreadyExistsException.class)
    protected ResponseEntity<Object> handleRegularAlreadyExist(RegularAlreadyExistsException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT, "Regular payment already exists.", ex));
    }

    @ExceptionHandler(InvalidRegularIdException.class)
    protected ResponseEntity<Object> handleRegularIdException(InvalidRegularIdException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT, "Regular payment invalid id.", ex));
    }

    @ExceptionHandler(InvalidTransactionSearchException.class)
    protected ResponseEntity<Object> handleRegularIdException(InvalidTransactionSearchException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT, "Regular payment invalid id.", ex));
    }

    @ExceptionHandler(EmailGenerationException.class)
    protected ResponseEntity<Object> handleEmailException(EmailGenerationException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FAILED_DEPENDENCY, "Email generation failed.", ex));
    }

    @ExceptionHandler(TranscoderException.class)
    protected ResponseEntity<Object> handleTranscoderException(TranscoderException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FAILED_DEPENDENCY, "Report failed, transcoder failure.", ex));
    }

    @ExceptionHandler(DocumentException.class)
    protected ResponseEntity<Object> handleTranscoderException(DocumentException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FAILED_DEPENDENCY, "Report failed, document exception.", ex));
    }

    @ExceptionHandler(IOException.class)
    protected ResponseEntity<Object> handleTranscoderException(IOException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.FAILED_DEPENDENCY, "IO Exception.", ex));
    }

    @ExceptionHandler(FileNotFoundException.class)
    protected ResponseEntity<Object> handleFileNotFound(FileNotFoundException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.NOT_FOUND,"File not found", ex));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    protected ResponseEntity<Object> handleInvalidTransaction(InvalidTransactionException ex) {
        return buildResponseEntity(new ApiError(HttpStatus.CONFLICT,"Invalid Transaction", ex));
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError,apiError.getStatus());
    }
}
