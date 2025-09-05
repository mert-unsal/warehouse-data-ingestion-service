package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
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
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Controller", description = "Handles inventory data ingestion and management")
public class InventoryController {

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public InventoryController(ObjectMapper objectMapper, KafkaProducerService kafkaProducerService) {
        this.objectMapper = objectMapper;
        this.kafkaProducerService = kafkaProducerService;
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

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // Parse the uploaded JSON file
            InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);

            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(inventoryData);
            kafkaProducerService.sendMessage("INVENTORY_UPLOAD: " + jsonMessage);

            return ResponseEntity.ok(String.format("Inventory uploaded successfully. %d articles processed.",
                inventoryData.inventory().size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing inventory file: " + e.getMessage());
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
        try {
            // Send to Kafka for downstream processing
            String jsonMessage = objectMapper.writeValueAsString(inventoryData);
            kafkaProducerService.sendMessage("INVENTORY_DATA: " + jsonMessage);

            return ResponseEntity.ok(String.format("Inventory data processed successfully. %d articles received.",
                inventoryData.inventory().size()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing inventory data: " + e.getMessage());
        }
    }
}
