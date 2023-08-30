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
    @ExceptionHandler(CreateAccountException.class)
    protected ResponseEntity<Object> handleCreateAccountException(CreateAccountException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(UpdateDeleteAccountException.class)
    protected ResponseEntity<Object> handleUpdateDeleteAccountException(UpdateDeleteAccountException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(CreateCategoryException.class)
    protected ResponseEntity<Object> handleCreateCategoryException(CreateCategoryException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(UpdateDeleteCategoryException.class)
    protected ResponseEntity<Object> handleUpdateDeleteCategoryException(UpdateDeleteCategoryException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(InvalidStatementIdException.class)
    protected ResponseEntity<Object> handleInvalidStatementIdException(InvalidStatementIdException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(StatementAlreadyExistsException.class)
    protected ResponseEntity<Object> handleStatementAlreadyExistsException(StatementAlreadyExistsException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(StatementAlreadyLockedException.class)
    protected ResponseEntity<Object> handleStatementAlreadyLockedException(StatementAlreadyLockedException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(CannotDeleteLockedStatementException.class)
    protected ResponseEntity<Object> handleCannotDeleteLockedStatementException(CannotDeleteLockedStatementException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(CannotDeleteLastStatementException.class)
    protected ResponseEntity<Object> handleCannotDeleteLastStatementException(CannotDeleteLastStatementException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(RegularAlreadyExistsException.class)
    protected ResponseEntity<Object> handleRegularAlreadyExistsException(RegularAlreadyExistsException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(InvalidRegularIdException.class)
    protected ResponseEntity<Object> handleInvalidRegularIdException(InvalidRegularIdException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(InvalidTransactionSearchException.class)
    protected ResponseEntity<Object> handleInvalidTransactionSearchException(InvalidTransactionSearchException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(EmailGenerationException.class)
    protected ResponseEntity<Object> handleEmailGenerationException(EmailGenerationException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(TranscoderException.class)
    protected ResponseEntity<Object> handleTranscoderException(TranscoderException ex) {
        return buildResponseEntity(new MoneyException(HttpStatus.FAILED_DEPENDENCY, ex));
    }

    @ExceptionHandler(DocumentException.class)
    protected ResponseEntity<Object> handleDocumentException(DocumentException ex) {
        return buildResponseEntity(new MoneyException(HttpStatus.FAILED_DEPENDENCY, ex));
    }

    @ExceptionHandler(IOException.class)
    protected ResponseEntity<Object> handleIOException(IOException ex) {
        return buildResponseEntity(new MoneyException(HttpStatus.FAILED_DEPENDENCY, ex));
    }

    @ExceptionHandler(FileNotFoundException.class)
    protected ResponseEntity<Object> handleFileNotFoundException(FileNotFoundException ex) {
        return buildResponseEntity(new MoneyException(HttpStatus.NOT_FOUND, ex));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    protected ResponseEntity<Object> handleInvalidTransactionException(InvalidTransactionException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(CannotDetermineNextDateException.class)
    protected ResponseEntity<Object> handleCannotDetermineNextDateException(CannotDetermineNextDateException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(InvalidTransactionIdException.class)
    protected ResponseEntity<Object> handleInvalidTransactionIdException(InvalidTransactionIdException ex) {
        return buildResponseEntity(ex);
    }

    @ExceptionHandler(MultipleUnlockedStatementException.class)
    protected ResponseEntity<Object> handleMultipleUnlockedStatementException(MultipleUnlockedStatementException ex) {
        return buildResponseEntity(ex);
    }

    private ResponseEntity<Object> buildResponseEntity(MoneyException error) {
        return new ResponseEntity<>(error,error.getStatus());
    }
}
