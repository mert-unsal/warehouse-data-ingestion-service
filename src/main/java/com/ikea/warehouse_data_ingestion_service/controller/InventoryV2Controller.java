package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import com.ikea.warehouse_data_ingestion_service.service.MetricsService;
import com.ikea.warehouse_data_ingestion_service.service.OpenTelemetryMetricsService;
import com.ikea.warehouse_data_ingestion_service.util.TraceContext;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/inventory")
@Tag(name = "Inventory V2 Controller", description = "Demonstrates OpenTelemetry vs Micrometer approaches")
public class InventoryV2Controller {

    private static final Logger logger = LoggerFactory.getLogger(InventoryV2Controller.class);

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    // Both services for comparison
    private final MetricsService micrometerMetricsService;
    private final OpenTelemetryMetricsService otelMetricsService;

    public InventoryV2Controller(ObjectMapper objectMapper,
                                KafkaProducerService kafkaProducerService,
                                MetricsService micrometerMetricsService,
                                OpenTelemetryMetricsService otelMetricsService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.micrometerMetricsService = micrometerMetricsService;
        this.otelMetricsService = otelMetricsService;
    }

    @Operation(
        summary = "Upload inventory with OpenTelemetry metrics",
        description = "Demonstrates OpenTelemetry approach with automatic tracing and custom metrics"
    )
    @ApiResponse(responseCode = "200", description = "Inventory uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping(value = "/upload/otel", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadInventoryOTel(
        @Parameter(description = "Inventory JSON file", required = true)
        @RequestParam("file") MultipartFile file) {

        // OpenTelemetry approach - automatic tracing + custom metrics
        try (var timedOperation = otelMetricsService.startFileUploadOperation("file_upload", "inventory")) {

            // Add custom attributes to current span
            Span currentSpan = Span.current();
            currentSpan.setAttribute("file.name", file.getOriginalFilename());
            currentSpan.setAttribute("file.size", file.getSize());
            currentSpan.setAttribute("operation.type", "inventory_upload");

            logger.info("Starting inventory file upload (OpenTelemetry) - filename: {}, size: {} bytes",
                       file.getOriginalFilename(), file.getSize());

            try {
                if (file.isEmpty()) {
                    otelMetricsService.recordFailedUpload("inventory", "empty_file");
                    timedOperation.recordError("Empty file provided");
                    return ResponseEntity.badRequest().body("File is empty");
                }

                // Parse the uploaded JSON file
                InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);
                currentSpan.setAttribute("items.parsed", inventoryData.inventory().size());

                logger.info("Successfully parsed inventory file - {} articles found",
                           inventoryData.inventory().size());

                // Send to Kafka
                kafkaProducerService.sendInventoryData(inventoryData);

                // Record successful metrics - OpenTelemetry will automatically correlate with trace
                otelMetricsService.recordSuccessfulUpload("inventory", inventoryData.inventory().size(), file.getSize());
                otelMetricsService.recordKafkaMessage("INVENTORY_UPLOAD");
                timedOperation.recordSuccess(inventoryData.inventory().size());

                logger.info("Inventory upload completed successfully - {} articles processed",
                           inventoryData.inventory().size());

                return ResponseEntity.ok(String.format(
                    "Inventory uploaded successfully using OpenTelemetry. %d articles processed. Trace ID: %s",
                    inventoryData.inventory().size(), currentSpan.getSpanContext().getTraceId()));

            } catch (Exception e) {
                otelMetricsService.recordFailedUpload("inventory", "processing_error");
                timedOperation.recordError("Processing failed: " + e.getMessage());

                logger.error("Inventory upload failed - error: {}", e.getMessage(), e);
                return ResponseEntity.badRequest().body("Error processing inventory file: " + e.getMessage());
            }
        }
    }

    @Operation(
        summary = "Upload inventory with Micrometer metrics (legacy)",
        description = "Demonstrates your current Micrometer approach for comparison"
    )
    @ApiResponse(responseCode = "200", description = "Inventory uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping(value = "/upload/micrometer", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadInventoryMicrometer(
        @Parameter(description = "Inventory JSON file", required = true)
        @RequestParam("file") MultipartFile file) {

        // Your current Micrometer approach
        String traceId = TraceContext.getCurrentTraceId();
        Timer.Sample timer = micrometerMetricsService.startFileUploadTimer();

        logger.info("Starting inventory file upload (Micrometer) - filename: {}, size: {} bytes, traceId: {}",
                   file.getOriginalFilename(), file.getSize(), traceId);

        try {
            if (file.isEmpty()) {
                logger.warn("Inventory upload failed - empty file provided, traceId: {}", traceId);
                micrometerMetricsService.recordFailedUpload("inventory", "empty_file");
                return ResponseEntity.badRequest().body("File is empty");
            }

            InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);

            logger.info("Successfully parsed inventory file - {} articles found, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            kafkaProducerService.sendInventoryData(inventoryData);

            // Manual metric recording
            micrometerMetricsService.recordSuccessfulUpload("inventory", inventoryData.inventory().size(), file.getSize());
            micrometerMetricsService.recordKafkaMessage("INVENTORY_UPLOAD");

            logger.info("Inventory upload completed successfully - {} articles processed, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            return ResponseEntity.ok(String.format(
                "Inventory uploaded successfully using Micrometer. %d articles processed. TraceId: %s",
                inventoryData.inventory().size(), traceId));

        } catch (Exception e) {
            logger.error("Inventory upload failed - error: {}, traceId: {}", e.getMessage(), traceId, e);
            micrometerMetricsService.recordFailedUpload("inventory", "processing_error");
            return ResponseEntity.badRequest().body("Error processing inventory file: " + e.getMessage());
        } finally {
            micrometerMetricsService.stopFileUploadTimer(timer);
        }
    }
}
