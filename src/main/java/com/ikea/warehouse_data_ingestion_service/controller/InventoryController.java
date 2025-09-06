package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorMessages.INVENTORY_UPLOADED_SUCCESS;

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
        @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Starting inventory file upload - filename: {}, size: {} bytes",
                   file.getOriginalFilename(), file.getSize());

        inventoryService.proceedFile(file, Instant.now());

        return ResponseEntity.ok(INVENTORY_UPLOADED_SUCCESS);
    }

}
