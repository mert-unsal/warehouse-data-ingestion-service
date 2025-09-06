package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.service.InventoryService;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory Controller", description = "Handles inventory data ingestion and management")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(
        summary = "Upload inventory JSON file",
        description = "Uploads and processes an inventory JSON file containing article stock information"
    )
    @ApiResponse(responseCode = "200", description = "Inventory uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadInventory(
        @Parameter(description = "Inventory JSON file", required = true, content = @Content(mediaType = "application/json"))
        @RequestParam("file") MultipartFile file) throws Exception {

        log.info("Starting inventory file upload - filename: {}, size: {} bytes",
                   file.getOriginalFilename(), file.getSize());

        InventoryData inventoryData = inventoryService.proceedFile(file);
        int processedCount = (inventoryData != null && inventoryData.inventory() != null) ? inventoryData.inventory().size() : 0;

        log.info("Inventory upload completed successfully");

        return ResponseEntity.ok(String.format("%s %d articles processed.", ErrorMessages.INVENTORY_UPLOADED_SUCCESS, processedCount));
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

        if (inventoryData == null || inventoryData.inventory() == null) {
            log.warn("Inventory data ingestion failed - invalid or null data provided");
            throw new FileProcessingException(ErrorMessages.INVALID_INVENTORY_DATA, "FILE_PROCESSING_ERROR");
        }

        log.info("Starting inventory data ingestion - {} articles received",
                   inventoryData.inventory().size());

        inventoryService.publishInventoryData(inventoryData);

        log.info("Inventory data ingestion completed successfully - {} articles processed",
                   inventoryData.inventory().size());

        return ResponseEntity.ok(String.format("%s %d articles received.", ErrorMessages.INVENTORY_DATA_PROCESSED_SUCCESS, inventoryData.inventory().size()));
    }
}
