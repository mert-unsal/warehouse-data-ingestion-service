package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import com.ikea.warehouse_data_ingestion_service.service.MetricsService;
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
@RequestMapping("/api/v1/products")
@Tag(name = "Product Controller", description = "Handles product data ingestion and management")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    private final MetricsService metricsService;

    public ProductController(ObjectMapper objectMapper, KafkaProducerService kafkaProducerService,
                             MetricsService metricsService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
        this.metricsService = metricsService;
    }

    @Operation(
        summary = "Upload products JSON file",
        description = "Uploads and processes a products JSON file containing product definitions and their required articles"
    )
    @ApiResponse(responseCode = "200", description = "Products uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadProducts(
        @Parameter(description = "Products JSON file", required = true)
        @RequestParam("file") MultipartFile file) {

        Timer.Sample timer = metricsService.startFileUploadTimer();
        logger.info("Starting product file upload - filename: {}, size: {} bytes",
                   file.getOriginalFilename(), file.getSize());

        try {
            if (file.isEmpty()) {
                logger.warn("Product upload failed - empty file provided");
                metricsService.recordFailedUpload("products", "empty_file");
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse the uploaded JSON file
            ProductsData productsData = objectMapper.readValue(file.getInputStream(), ProductsData.class);

            logger.info("Successfully parsed products file - {} products found",
                       productsData.products().size());

            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(productsData);
            kafkaProducerService.sendMessage("PRODUCT_UPLOAD: " + jsonMessage);

            // Record metrics with improved method
            metricsService.recordSuccessfulUpload("products", productsData.products().size(), file.getSize());
            metricsService.recordKafkaMessage("PRODUCT_UPLOAD");

            logger.info("Product upload completed successfully - {} products processed",
                       productsData.products().size());

            return ResponseEntity.ok(String.format("Products uploaded successfully. %d products processed.",
                productsData.products().size()));

        } catch (Exception e) {
            logger.error("Product upload failed - error: {}", e.getMessage(), e);
            metricsService.recordFailedUpload("products", "processing_error");
            return ResponseEntity.badRequest().body("Error processing products file: " + e.getMessage());
        } finally {
            metricsService.stopFileUploadTimer(timer);
        }
    }

    @Operation(
        summary = "Upload products via JSON payload",
        description = "Accepts products data directly as JSON payload"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Products processed successfully",
        content = @Content(schema = @Schema(implementation = String.class))
    )
    @PostMapping("/data")
    public ResponseEntity<String> uploadProductsData(@RequestBody ProductsData productsData) {
        Timer.Sample timer = metricsService.startDataProcessingTimer();

        try {
            // Add null safety check
            if (productsData == null || productsData.products() == null) {
                logger.warn("Product data ingestion failed - invalid or null data provided");
                metricsService.recordFailedUpload("products", "invalid_data");
                return ResponseEntity.badRequest().body("Invalid products data provided");
            }

            logger.info("Starting product data ingestion - {} products received",
                       productsData.products().size());

            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(productsData);
            kafkaProducerService.sendMessage("PRODUCT_DATA: " + jsonMessage);

            // Record metrics with improved method
            metricsService.recordSuccessfulUpload("products", productsData.products().size(), 0L);
            metricsService.recordKafkaMessage("PRODUCT_DATA");

            logger.info("Product data ingestion completed successfully - {} products processed",
                       productsData.products().size());

            return ResponseEntity.ok(String.format("Products data processed successfully. %d products received.",
                productsData.products().size()));

        } catch (Exception e) {
            logger.error("Product data ingestion failed - error: {}", e.getMessage(), e);
            metricsService.recordFailedUpload("products", "processing_error");
            return ResponseEntity.badRequest().body("Error processing products data: " + e.getMessage());
        } finally {
            metricsService.stopDataProcessingTimer(timer);
        }
    }
}
