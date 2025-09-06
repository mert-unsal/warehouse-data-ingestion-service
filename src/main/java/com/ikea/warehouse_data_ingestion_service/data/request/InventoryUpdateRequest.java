package com.ikea.warehouse_data_ingestion_service.data.request;

import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request payload to submit inventory updates")
public record InventoryUpdateRequest(
    @Schema(description = "List of inventory items to ingest")
    @NotEmpty(message = "inventory must not be empty")
    List<InventoryItem> inventory
) {}
