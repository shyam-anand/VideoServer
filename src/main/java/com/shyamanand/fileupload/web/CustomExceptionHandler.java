package com.shyamanand.fileupload.web;

import com.shyamanand.fileupload.web.models.ApiResponse;
import com.shyamanand.fileupload.web.models.ErrorDetails;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         03/09/17
 */
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);

    @ExceptionHandler(value = FileNotFoundException.class)
    protected ResponseEntity notFoundException(Exception ex, WebRequest request) {
        logger.error("Not found exception: {}", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setTitle("File not found");
        errorDetails.setDetails(ex.getMessage());
        return handleExceptionInternal(ex, new ApiResponse(errorDetails), new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = {FileUploadException.class, IllegalStateException.class, IllegalArgumentException.class})
    protected ResponseEntity fileUploadExceptionHandler(RuntimeException ex, WebRequest request) {
        logger.error("Exception: ", ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails();
        if (ex.getMessage().contains("exceeds its maximum permitted size")) {
            errorDetails.setTitle("File too large");
            errorDetails.setDetails("File exceeds permitted size");
        } else {
            errorDetails.setDetails(ex.getMessage());
        }
        return handleExceptionInternal(ex, new ApiResponse<>(errorDetails), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = {IOException.class, RuntimeException.class, Exception.class})
    protected ResponseEntity exceptionHandler(RuntimeException ex, WebRequest request) {
        logger.error("Generic exception: " + ex.getMessage());
        ErrorDetails errorDetails = new ErrorDetails();
        errorDetails.setDetails(ex.getMessage());
        return handleExceptionInternal(ex, new ApiResponse<>(errorDetails), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
