package com.ikea.warehouse_data_ingestion_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.product}")
    private String productTopic;

    @Value("${app.kafka.topics.inventory}")
    private String inventoryTopic;

    /**
     * Send a message to Kafka.
     * Tracing is handled automatically by the instrumented KafkaTemplate.
     */
    public CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object message) {
        return sendMessage(topic, key, message, null);
    }

    /**
     * Send a message to Kafka with custom headers.
     * Tracing is handled automatically by the instrumented KafkaTemplate.
     */
    public CompletableFuture<SendResult<String, Object>> sendMessage(String topic, String key, Object message, Map<String, String> customHeaders) {
        log.info("Sending Kafka message to topic '{}' with key: '{}'", topic, key);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, message);

        // Add custom headers if provided
        if (customHeaders != null) {
            customHeaders.forEach((headerKey, headerValue) ->
                    record.headers().add(headerKey, headerValue.getBytes(StandardCharsets.UTF_8)));
        }

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        // Add success and error callbacks
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send message to topic '{}' with key '{}': {}",
                        topic, key, throwable.getMessage(), throwable);
            } else {
                log.info("Successfully sent message to topic '{}' with key '{}', partition: {}, offset: {}",
                        topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });

        return future;
    }

    /**
     * Send product update message.
     * Tracing context is automatically propagated by Spring Boot instrumentation.
     */
    public CompletableFuture<SendResult<String, Object>> sendProductUpdate(String productId, Object productData) {
        log.info("Sending product update for product ID: {}", productId);
        return sendMessage(productTopic, productId, productData, null);
    }

    /**
     * Send inventory update message.
     * Tracing context is automatically propagated by Spring Boot instrumentation.
     */
    public CompletableFuture<SendResult<String, Object>> sendInventoryUpdate(String inventoryId, Object inventoryData) {
        log.info("Sending inventory update for inventory ID: {}", inventoryId);
        return sendMessage(inventoryTopic, inventoryId, inventoryData, null);
    }
}
