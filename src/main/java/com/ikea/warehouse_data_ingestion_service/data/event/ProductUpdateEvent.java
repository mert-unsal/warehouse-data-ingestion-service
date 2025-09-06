package com.ikea.warehouse_data_ingestion_service.data.event;

import com.ikea.warehouse_data_ingestion_service.data.dto.Product;

import java.util.List;

public record ProductUpdateEvent(
    List<Product> products,
    Long timestamp
) {}
