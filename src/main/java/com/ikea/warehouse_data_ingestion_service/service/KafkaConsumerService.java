package com.ikea.warehouse_data_ingestion_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    @KafkaListener(topics = "${kafka.topic}", groupId = "warehouse-group")
    public void listen(String message) {
        // For now, just log the message
        System.out.println("Received message: " + message);
    }
}
