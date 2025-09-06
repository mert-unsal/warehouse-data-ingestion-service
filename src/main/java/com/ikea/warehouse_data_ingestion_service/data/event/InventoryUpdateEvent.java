package com.ikea.warehouse_data_ingestion_service.data.event;

import lombok.Builder;

import java.time.Instant;

@Builder(toBuilder = true)
public record InventoryUpdateEvent(String artId, String name, String stock, Instant fileCreatedAt) {}
