package com.ikea.warehouse_data_ingestion_service.data.event;

import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryItem;

import java.util.List;

public record InventoryUpdateEvent(
    List<InventoryItem> inventory,
    Long timestamp
) {}
