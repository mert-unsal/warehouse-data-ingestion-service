package com.ikea.warehouse_data_ingestion_service.data.event;

public record KafkaCommonErrorEvent(
    String key,
    Object originalMessage,
    String originalTopic,
    String errorMessage,
    Long timestamp
) {}
