package com.ikea.warehouse_data_ingestion_service.data;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Wrapper for products data containing list of products")
public record ProductsData(
    @Schema(description = "List of products")
    List<Product> products
) {}
