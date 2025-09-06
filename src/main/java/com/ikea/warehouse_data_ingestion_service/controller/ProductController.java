package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.service.ProductService;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
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
        @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Starting product file upload - filename: {}, size: {} bytes",
                   file.getOriginalFilename(), file.getSize());

        productService.proceedFile(file, Instant.now());

        return ResponseEntity.ok(ErrorMessages.PRODUCTS_UPLOADED_SUCCESS);
    }

}
