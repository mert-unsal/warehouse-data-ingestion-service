package com.ikea.warehouse_data_ingestion_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Inventory item representing a warehouse article")
public record InventoryItem(
    @JsonProperty("art_id")
    @Schema(description = "Unique article identifier", example = "1")
    String artId,

    @Schema(description = "Name of the inventory item", example = "leg")
    String name,

    @Schema(description = "Available stock quantity", example = "12")
    String stock
) {}
