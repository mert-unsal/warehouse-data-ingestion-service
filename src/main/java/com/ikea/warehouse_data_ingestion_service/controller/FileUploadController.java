package com.ikea.warehouse_data_ingestion_service.controller;

import com.ikea.warehouse_data_ingestion_service.service.FileIngestionService;
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
public class FileUploadController {

    private final FileIngestionService fileIngestionService;

    public FileUploadController(FileIngestionService fileIngestionService) {
        this.fileIngestionService = fileIngestionService;
    }

    @PostMapping(value = "/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> ingest(
            @RequestPart(value = "inventory", required = false) MultipartFile inventory,
            @RequestPart(value = "products", required = false) MultipartFile products) throws IOException {

        if ((inventory == null || inventory.isEmpty()) && (products == null || products.isEmpty())) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "At least one file (inventory or products) must be provided");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return ResponseEntity.ok(fileIngestionService.ingest(inventory, products));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "FILE_PARSING_ERROR");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}

