package com.ikea.warehouse_data_ingestion_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendMessage(Object message) {
        sendMessage(null, message);
    }

    public void sendMessage(String key, Object message) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Sent message=[{}] with key=[{}] to topic=[{}] with offset=[{}]",
                            message.getClass().getSimpleName(), key, topic, result.getRecordMetadata().offset());
                } else {
                    logger.error("Unable to send message=[{}] with key=[{}] to topic=[{}] due to: {}",
                            message.getClass().getSimpleName(), key, topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message to Kafka: {}", e.getMessage(), e);
        }
    }

    public void sendInventoryData(Object inventoryData) {
        sendMessage("inventory", inventoryData);
    }

    public void sendProductData(Object productData) {
        sendMessage("product", productData);
    }
}
