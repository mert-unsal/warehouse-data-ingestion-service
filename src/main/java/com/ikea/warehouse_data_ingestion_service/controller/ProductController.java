package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
@Tag(name = "Product Controller", description = "Handles product data ingestion and management")
public class ProductController {

    private final ProductService productService;

    @Operation(
        summary = "Upload products JSON file",
        description = "Uploads and processes a products JSON file containing product definitions and their required articles"
    )
    @ApiResponse(responseCode = "200", description = "Products uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid file format or content")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadProducts(
        @Parameter(
            description = "Products JSON file",
            required = true,
            content = @Content(mediaType = "application/json")
        )
        @RequestParam("file") MultipartFile file) throws Exception {

        log.info("Starting product file upload - filename: {}, size: {} bytes",
                   file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            log.warn("Product upload failed - empty file provided");
            throw new FileProcessingException("File is empty", "FILE_PROCESSING_ERROR");
        }

        productService.proceedFile(file);
        log.info("Product upload completed successfully");
        return ResponseEntity.ok("Products uploaded successfully.");
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

        if (productsData == null || productsData.products() == null) {
            log.warn("Product data ingestion failed - invalid or null data provided");
            throw new FileProcessingException("Invalid products data provided", "FILE_PROCESSING_ERROR");
        }

        log.info("Starting product data ingestion - {} products received",
                   productsData.products().size());

        productService.publishProductData(productsData);

        log.info("Product data ingestion completed successfully - {} products processed",
                   productsData.products().size());

        return ResponseEntity.ok(String.format("Products data processed successfully. %d products received.",
            productsData.products().size()));
    }
}
