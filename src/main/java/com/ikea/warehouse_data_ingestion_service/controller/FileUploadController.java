package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.service.FileIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Ingestion", description = "Endpoints for uploading and processing warehouse data files")
public class FileUploadController {

    private final FileIngestionService fileIngestionService;

    public FileUploadController(FileIngestionService fileIngestionService) {
        this.fileIngestionService = fileIngestionService;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Ingest warehouse data files",
            description = "Upload inventory and/or products JSON files for processing. At least one file must be provided.",
            operationId = "ingestFiles"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Files successfully processed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                              "inventoryCount": 4,
                                              "productCount": 2,
                                              "status": "INGESTED"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - missing files or invalid JSON format",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = {
                                    @ExampleObject(
                                            name = "No Files Provided",
                                            value = """
                                                    {
                                                      "message": "At least one file (inventory or products) must be provided"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "JSON Parsing Error",
                                            value = """
                                                    {
                                                      "error": "FILE_PROCESSING_ERROR",
                                                      "message": "Failed to process uploaded file: Invalid JSON format",
                                                      "status": 400,
                                                      "path": "/api/files/ingest",
                                                      "timestamp": "2025-09-05T12:15:55"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    public ResponseEntity<?> ingest(
            @Parameter(
                    description = "Inventory JSON file with structure: {\"inventory\": [{\"art_id\": \"1\", \"name\": \"Item\", \"stock\": \"10\"}]}",
                    content = @Content(mediaType = "application/json")
            )
            @RequestPart(value = "inventory", required = false) MultipartFile inventory,

            @Parameter(
                    description = "Products JSON file with structure: {\"products\": [{\"name\": \"Product\", \"contain_articles\": [{\"art_id\": \"1\", \"amount_of\": \"2\"}]}]}",
                    content = @Content(mediaType = "application/json")
            )
            @RequestPart(value = "products", required = false) MultipartFile products) throws IOException {

        if ((inventory == null || inventory.isEmpty()) && (products == null || products.isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "At least one file (inventory or products) must be provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return ResponseEntity.ok(fileIngestionService.ingest(inventory, products));
    }
}
