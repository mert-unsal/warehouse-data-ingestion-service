package com.ikea.warehouse_data_ingestion_service.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard error response structure")
public record ErrorResponse(
    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    String error,

    @Schema(description = "Human readable error message", example = "Invalid file format")
    String message,

    @Schema(description = "HTTP status code", example = "400")
    int status,

    @Schema(description = "Request path where error occurred", example = "/api/files/ingest")
    String path,

    @Schema(description = "Timestamp of when error occurred", example = "2025-09-05T12:15:55")
    String timestamp
) {}
