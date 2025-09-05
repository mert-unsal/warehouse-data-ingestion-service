package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import com.ikea.warehouse_data_ingestion_service.service.MetricsService;
import com.ikea.warehouse_data_ingestion_service.util.TraceContext;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Controller", description = "Handles inventory data ingestion and management")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    private final MetricsService metricsService;

    public InventoryController(ObjectMapper objectMapper, KafkaProducerService kafkaProducerService,
                             MetricsService metricsService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
    }

    @Operation(
        summary = "Upload inventory JSON file",
        description = "Uploads and processes an inventory JSON file containing article stock information"
    )
    @ApiResponse(responseCode = "200", description = "Inventory uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadInventory(
        @Parameter(description = "Inventory JSON file", required = true)
        @RequestParam("file") MultipartFile file) {

        String traceId = TraceContext.getCurrentTraceId();
        Timer.Sample timer = metricsService.startFileUploadTimer();

        logger.info("Starting inventory file upload - filename: {}, size: {} bytes, traceId: {}",
                   file.getOriginalFilename(), file.getSize(), traceId);

        try {
            if (file.isEmpty()) {
                logger.warn("Inventory upload failed - empty file provided, traceId: {}", traceId);
                metricsService.recordFailedUpload("inventory", "empty_file");
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse the uploaded JSON file
            InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);

            logger.info("Successfully parsed inventory file - {} articles found, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            // Send to Kafka for downstream processing (trace ID will be added by interceptor)
            String jsonMessage = objectMapper.writeValueAsString(inventoryData);
            kafkaProducerService.sendMessage("INVENTORY_UPLOAD: " + jsonMessage);

            // Record metrics
            metricsService.recordSuccessfulUpload("inventory", inventoryData.inventory().size(), file.getSize());
            metricsService.recordKafkaMessage("INVENTORY_UPLOAD");

            logger.info("Inventory upload completed successfully - {} articles processed, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            return ResponseEntity.ok(String.format("Inventory uploaded successfully. %d articles processed. TraceId: %s",
                inventoryData.inventory().size(), traceId));

        } catch (Exception e) {
            logger.error("Inventory upload failed - error: {}, traceId: {}", e.getMessage(), traceId, e);
            metricsService.recordFailedUpload("inventory", "processing_error");
            return ResponseEntity.badRequest().body("Error processing inventory file: " + e.getMessage());
        } finally {
            metricsService.stopFileUploadTimer(timer);
        }
    }

    @Operation(
        summary = "Upload inventory via JSON payload",
        description = "Accepts inventory data directly as JSON payload"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Inventory processed successfully",
        content = @Content(schema = @Schema(implementation = String.class))
    )
    @PostMapping("/data")
    public ResponseEntity<String> uploadInventoryData(@RequestBody InventoryData inventoryData) {
        String traceId = TraceContext.getCurrentTraceId();
        Timer.Sample timer = metricsService.startDataProcessingTimer();

        try {
            // Add null safety check
            if (inventoryData == null || inventoryData.inventory() == null) {
                logger.warn("Inventory data ingestion failed - invalid or null data provided, traceId: {}", traceId);
                metricsService.recordFailedUpload("inventory", "invalid_data");
                return ResponseEntity.badRequest().body("Invalid inventory data provided");
            }

            logger.info("Starting inventory data ingestion - {} articles received, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(inventoryData);
            kafkaProducerService.sendMessage("INVENTORY_DATA: " + jsonMessage);

            // Record metrics
            metricsService.recordSuccessfulUpload("inventory", inventoryData.inventory().size(), 0L);
            metricsService.recordKafkaMessage("INVENTORY_DATA");

            logger.info("Inventory data ingestion completed successfully - {} articles processed, traceId: {}",
                       inventoryData.inventory().size(), traceId);

            return ResponseEntity.ok(String.format("Inventory data processed successfully. %d articles received. TraceId: %s",
                inventoryData.inventory().size(), traceId));

        } catch (Exception e) {
            logger.error("Inventory data ingestion failed - error: {}, traceId: {}", e.getMessage(), traceId, e);
            metricsService.recordFailedUpload("inventory", "processing_error");
            return ResponseEntity.badRequest().body("Error processing products data: " + e.getMessage());
        } finally {
            metricsService.stopDataProcessingTimer(timer);
        }
    }
}
