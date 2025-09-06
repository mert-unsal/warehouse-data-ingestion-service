package com.ikea.warehouse_data_ingestion_service.service;

import com.ikea.warehouse_data_ingestion_service.data.event.KafkaCommonErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

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

    @Value("${app.kafka.topics.product-error}")
    private String productErrorTopic;

    @Value("${app.kafka.topics.inventory-error}")
    private String inventoryErrorTopic;

    public void sendProductUpdate(String productId, Object productData) {
        log.info("Sending product update for product ID: {}", productId);
        sendMessage(productTopic, productId, productData);
    }

    public void sendInventoryUpdate(String inventoryId, Object inventoryData) {
        log.info("Sending inventory update for inventory ID: {}", inventoryId);
        sendMessage(inventoryTopic, inventoryId, inventoryData);
    }

    private void sendMessage(String topic, String key, Object message) {
        log.info("Sending Kafka message to topic '{}' with key: '{}'", topic, key);

        ProducerRecord<String, Object> record = new ProducerRecord<>(topic, key, message);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);

        // Add success and error callbacks
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send message to topic '{}' with key '{}': {}",
                        topic, key, throwable.getMessage(), throwable);
                // Send to error topic
                String errorTopic = topic.equals(productTopic) ? productErrorTopic : inventoryErrorTopic;
                sendErrorMessage(errorTopic, key, message, topic, throwable);
            } else {
                log.info("Successfully sent message to topic '{}' with key '{}', partition: {}, offset: {}",
                        topic, key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }

    private void sendErrorMessage(String errorTopic, String key, Object originalMessage, String originalTopic, Throwable throwable) {
        KafkaCommonErrorEvent kafkaCommonErrorEvent = new KafkaCommonErrorEvent(
                key,
                originalMessage,
                originalTopic,
                throwable.getMessage(),
                Long.valueOf(System.currentTimeMillis())
        );
        kafkaTemplate.send(errorTopic, key, kafkaCommonErrorEvent);
        log.info("Sent error message to error topic '{}' for key '{}'", errorTopic, key);
    }
}
