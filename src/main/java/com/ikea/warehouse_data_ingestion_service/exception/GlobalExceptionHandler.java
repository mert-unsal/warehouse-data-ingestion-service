package com.ikea.warehouse_data_ingestion_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorTypes.FILE_PROCESSING_ERROR;

@Slf4j
@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ioException, HttpServletRequest request) {
        log.error("IO Exception occurred: {}", ioException.getMessage(), ioException);

        ErrorResponse errorResponse = new ErrorResponse(
                FILE_PROCESSING_ERROR,
                STR."Failed to process uploaded file: \{ioException.getMessage()}",
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI(),
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(KafkaProduceFailedException.class)
    public ResponseEntity<ErrorResponse> handleKafkaMessage(KafkaProduceFailedException kafkaProduceFailedException, HttpServletRequest request) {
        log.error("KafkaProduceFailedException Exception occurred: {}", kafkaProduceFailedException.getMessage(), kafkaProduceFailedException);
        ErrorResponse errorResponse = new ErrorResponse(
            FILE_PROCESSING_ERROR, STR."Failed to process uploaded file: \{kafkaProduceFailedException.getMessage()}",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorResponse> handleJsonProcessingException(JsonProcessingException ex, HttpServletRequest request) {
        log.error("JSON Processing Exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_JSON_FORMAT",
            "Invalid JSON format in uploaded file: " + ex.getOriginalMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.error("HTTP Media Type Not Supported Exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "UNSUPPORTED_MEDIA_TYPE",
            "Content type not supported. Please use multipart/form-data for file uploads.",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        log.error("Multipart Exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "MULTIPART_ERROR",
            "Error processing multipart request: " + ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.error("Max upload size exceeded: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "FILE_TOO_LARGE",
            "Uploaded file exceeds maximum allowed size",
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<ErrorResponse> handleFileProcessingException(FileProcessingException ex, HttpServletRequest request) {
        log.error("FileProcessingException occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
            ex.getError(),
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.error("Illegal Argument Exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_ARGUMENT",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.error("Validation failed: {}", ex.getMessage(), ex);
        String uri = request.getRequestURI();
        String message = uri != null && uri.contains("/inventory")
                ? ErrorMessages.INVALID_INVENTORY_DATA
                : ErrorMessages.INVALID_PRODUCTS_DATA;
        ErrorResponse errorResponse = new ErrorResponse(
                FILE_PROCESSING_ERROR,
                message,
                HttpStatus.BAD_REQUEST.value(),
                uri,
                LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred. Please try again later.",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            request.getRequestURI(),
            LocalDateTime.now().format(TIMESTAMP_FORMATTER)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
