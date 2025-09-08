package com.ikea.warehouse_data_ingestion_service.exception;

import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    // helper method to build a MethodParameter for MethodArgumentNotValidException
    @SuppressWarnings("unused")
    private static void dummy(String arg) {}

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest request(String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI(uri);
        return req;
    }

    @Test
    void handleIOException_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleIOException(new IOException("boom"), request("/api/v1/products/upload"));
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("boom"));
    }

    @Test
    void handleKafkaMessage_returnsBadRequest() {
        ResponseEntity<ErrorResponse> response = handler.handleKafkaMessage(new KafkaProduceFailedException("kfail"), request("/api/v1/products/upload"));
        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().message().contains("kfail"));
    }

    @Test
    void handleFileProcessingException_mapsToBadRequest() {
        FileProcessingException ex = new FileProcessingException(ErrorMessages.FILE_EMPTY, "FILE_PROCESSING_ERROR");
        ResponseEntity<ErrorResponse> response = handler.handleFileProcessingException(ex, request("/api/v1/inventory/upload"));
        assertEquals(400, response.getStatusCode().value());
        assertEquals(ErrorMessages.FILE_EMPTY, response.getBody().message());
        assertEquals("FILE_PROCESSING_ERROR", response.getBody().error());
    }

    @Test
    void handleGenericException_returns500() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("x"), request("/x"));
        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().error());
    }

    @Test
    void handleJsonAndMultipartAndOthers_coverAll() throws Exception {
        ResponseEntity<ErrorResponse> json = handler.handleJsonProcessingException(new com.fasterxml.jackson.core.JsonParseException(null, "bad"), request("/api/v1/products/upload"));
        assertEquals(400, json.getStatusCode().value());
        assertNotNull(json.getBody());
        assertEquals("INVALID_JSON_FORMAT", json.getBody().error());

        ResponseEntity<ErrorResponse> media = handler.handleHttpMediaTypeNotSupportedException(new HttpMediaTypeNotSupportedException("oops"), request("/api/v1/products/upload"));
        assertEquals(415, media.getStatusCode().value());
        assertNotNull(media.getBody());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", media.getBody().error());

        ResponseEntity<ErrorResponse> multipart = handler.handleMultipartException(new MultipartException("merr"), request("/api/v1/products/upload"));
        assertEquals(400, multipart.getStatusCode().value());
        assertNotNull(multipart.getBody());
        assertEquals("MULTIPART_ERROR", multipart.getBody().error());

        ResponseEntity<ErrorResponse> tooLarge = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(1), request("/api/v1/products/upload"));
        assertEquals(413, tooLarge.getStatusCode().value());
        assertNotNull(tooLarge.getBody());
        assertEquals("FILE_TOO_LARGE", tooLarge.getBody().error());

        ResponseEntity<ErrorResponse> illegal = handler.handleIllegalArgumentException(new IllegalArgumentException("bad arg"), request("/x"));
        assertEquals(400, illegal.getStatusCode().value());
        assertNotNull(illegal.getBody());
        assertEquals("INVALID_ARGUMENT", illegal.getBody().error());
    }

    @Test
    void handleMethodArgumentNotValid_usesUriToPickMessage() throws Exception {
        org.springframework.core.MethodParameter mp = new org.springframework.core.MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        org.springframework.validation.BindException be = new org.springframework.validation.BindException(new Object(), "target");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, be.getBindingResult());

        ResponseEntity<ErrorResponse> inv = handler.handleMethodArgumentNotValid(ex, request("/api/v1/inventory/upload"));
        assertEquals(400, inv.getStatusCode().value());
        assertNotNull(inv.getBody());
        assertEquals(ErrorMessages.INVALID_INVENTORY_DATA, inv.getBody().message());

        ResponseEntity<ErrorResponse> prod = handler.handleMethodArgumentNotValid(ex, request("/api/v1/products/upload"));
        assertEquals(400, prod.getStatusCode().value());
        assertNotNull(prod.getBody());
        assertEquals(ErrorMessages.INVALID_PRODUCTS_DATA, prod.getBody().message());
    }
}
