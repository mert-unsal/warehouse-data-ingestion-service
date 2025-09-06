package com.ikea.warehouse_data_ingestion_service.data.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Wrapper for inventory data containing list of inventory items")
public record InventoryData(
    @Schema(description = "List of inventory items")
    List<InventoryItem> inventory
) {}
