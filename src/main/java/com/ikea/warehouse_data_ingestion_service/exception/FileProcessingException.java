package com.ikea.warehouse_data_ingestion_service.exception;

import lombok.Getter;

/**
 * Custom exception for file processing errors in the warehouse data ingestion service.
 * This exception will be caught by the GlobalExceptionHandler and converted to a proper ErrorResponse.
 */
@Getter
public class FileProcessingException extends RuntimeException {

    private final String error;

    public FileProcessingException(String message) {
        super(message);
        this.error = null;
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.error = null;
    }

    public FileProcessingException(String message, String error) {
        super(message);
        this.error = error;
    }

    public FileProcessingException(String message, String error, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

}
