package com.infy.tradeconnect.exception;

import com.infy.tradeconnect.dto.ErrorInfo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionControllerAdvice {

    @Autowired
    private Environment environment;

    // =========================================================================
    // 6.1.2. Exception Handling for TradeConnectException
    // =========================================================================
    @ExceptionHandler(TradeConnectException.class)
    public ResponseEntity<ErrorInfo> tradeConnectExceptionHandler(TradeConnectException exception) {
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());

        // Resolve error message from property files if keys are present
        String message = environment.getProperty(exception.getMessage(), exception.getMessage());
        errorInfo.setErrorMessage(message);
        errorInfo.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 6.1.3. Validation Exception Handling
    // =========================================================================
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorInfo> validatorExceptionHandler(Exception exception) {
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.BAD_REQUEST.value());
        errorInfo.setTimestamp(LocalDateTime.now());

        String errorMessage = "";

        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException manvException = (MethodArgumentNotValidException) exception;
            errorMessage = manvException.getBindingResult().getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
        } else if (exception instanceof ConstraintViolationException) {
            ConstraintViolationException cvException = (ConstraintViolationException) exception;
            errorMessage = cvException.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
        }

        errorInfo.setErrorMessage(errorMessage);
        return new ResponseEntity<>(errorInfo, HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // 6.1.4. General Exception Handling
    // =========================================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorInfo> generalExceptionHandler(Exception exception) {
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setErrorCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorInfo.setErrorMessage(environment.getProperty("general.exception", "An internal server error occurred while processing the request."));
        errorInfo.setTimestamp(LocalDateTime.now());

        return new ResponseEntity<>(errorInfo, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}