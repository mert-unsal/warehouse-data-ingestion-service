package com.ikea.warehouse_data_ingestion_service.data;

public record ErrorKafkaMessage(
    String key,
    Object originalMessage,
    String originalTopic,
    String errorMessage,
    long timestamp
) {}
