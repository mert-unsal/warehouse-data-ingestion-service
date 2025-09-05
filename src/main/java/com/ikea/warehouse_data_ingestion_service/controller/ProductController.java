package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Controller", description = "Handles product data ingestion and management")
public class ProductController {

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public ProductController(ObjectMapper objectMapper, KafkaProducerService kafkaProducerService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
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

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse the uploaded JSON file
            ProductsData productsData = objectMapper.readValue(file.getInputStream(), ProductsData.class);

            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(productsData);
            kafkaProducerService.sendMessage("PRODUCT_UPLOAD: " + jsonMessage);

            return ResponseEntity.ok(String.format("Products uploaded successfully. %d products processed.",
                productsData.products().size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing products file: " + e.getMessage());
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
        try {
            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(productsData);
            kafkaProducerService.sendMessage("PRODUCT_DATA: " + jsonMessage);

            return ResponseEntity.ok(String.format("Products data processed successfully. %d products received.",
                productsData.products().size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing products data: " + e.getMessage());
        }
    }
}
