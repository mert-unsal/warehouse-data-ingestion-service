package com.ikea.warehouse_data_ingestion_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Profile("!test")
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.product}")
    private String productTopic;

    @Value("${kafka.topic.product}")
    private String inventoryTopic;

    public void sendProductData(Object productData) {
        sendMessage(productTopic, "product", productData);
    }

    public void sendInventoryData(Object inventoryData) {
        sendMessage(inventoryTopic, "inventory", inventoryData);
    }

    private void sendMessage(String topic, String key, Object message) {
        try {
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent message=[{}] with key=[{}] to topic=[{}] with offset=[{}]",
                            message.getClass().getSimpleName(), key, topic, result.getRecordMetadata().offset());
                } else {
                    log.error("Unable to send message=[{}] with key=[{}] to topic=[{}] due to: {}",
                            message.getClass().getSimpleName(), key, topic, ex.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Error sending message to Kafka: {}", e.getMessage(), e);
        }
    }
}
