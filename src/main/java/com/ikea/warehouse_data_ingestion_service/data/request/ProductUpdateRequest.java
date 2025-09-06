package com.ikea.warehouse_data_ingestion_service.data.request;

import com.ikea.warehouse_data_ingestion_service.data.dto.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request payload to submit product updates")
public record ProductUpdateRequest(
    @Schema(description = "List of products to ingest")
    @NotEmpty(message = "products must not be empty")
    List<Product> products
) {}
