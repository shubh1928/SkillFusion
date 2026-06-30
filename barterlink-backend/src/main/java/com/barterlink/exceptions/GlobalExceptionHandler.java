package com.barterlink.exceptions;

import com.barterlink.dtos.ApiError;
import com.barterlink.dtos.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


//This will only run if the exceptions are generated through servlet(only run for servlet level exception)
//@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            CredentialsExpiredException.class,
            DisabledException.class
    })
    public ResponseEntity<ApiError> handleAuthException(Exception e, HttpServletRequest request){

        logger.info("Exception : {}", e.getClass().getName());

        var apiError = ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", e.getMessage(), request.getRequestURI());
        return ResponseEntity.badRequest().body(apiError);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception){
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), HttpStatus.NOT_FOUND,404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception){
        ErrorResponse errorResponse = new ErrorResponse(exception.getMessage(), HttpStatus.BAD_REQUEST,400);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

}
