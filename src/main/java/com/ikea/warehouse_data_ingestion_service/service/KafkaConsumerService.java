package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.util.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final ObjectMapper objectMapper;

    public KafkaConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "${kafka.consumer.group-id}")
    public void listen(@Payload Object message,
                      @Header(KafkaHeaders.RECEIVED_KEY) String messageKey) {

        String traceId = TraceContext.getCurrentTraceId();

        try {
            logger.info("Received Kafka message with key: {}, type: {}, traceId: {}",
                    messageKey, message.getClass().getSimpleName(), traceId);

            // Handle different message types based on the message key
            if (messageKey != null) {
                switch (messageKey.toLowerCase()) {
                    case "inventory" -> handleInventoryMessage(message, traceId);
                    case "product" -> handleProductMessage(message, traceId);
                    default -> logger.warn("Unknown message key: {}, traceId: {}", messageKey, traceId);
                }
            } else {
                logger.warn("Received message without key, attempting to process as generic object, traceId: {}", traceId);
                handleGenericMessage(message, traceId);
            }
        } catch (Exception e) {
            logger.error("Error processing Kafka message with key: {}, traceId: {}, error: {}",
                    messageKey, traceId, e.getMessage(), e);
        }
    }

    private void handleInventoryMessage(Object message, String traceId) {
        try {
            InventoryData inventoryData;

            if (message instanceof Map) {
                // Convert Map to InventoryData
                inventoryData = objectMapper.convertValue(message, InventoryData.class);
            } else if (message instanceof InventoryData) {
                inventoryData = (InventoryData) message;
            } else {
                throw new IllegalArgumentException("Unexpected message type: " + message.getClass());
            }

            logger.info("Processing inventory message: {} articles received, traceId: {}",
                    inventoryData.inventory().size(), traceId);

            // Process inventory data
            processInventoryData(inventoryData, traceId);

        } catch (Exception e) {
            logger.error("Error processing inventory message, traceId: {}, error: {}", traceId, e.getMessage(), e);
        }
    }

    private void handleProductMessage(Object message, String traceId) {
        try {
            ProductsData productsData;

            if (message instanceof Map) {
                // Convert Map to ProductsData
                productsData = objectMapper.convertValue(message, ProductsData.class);
            } else if (message instanceof ProductsData) {
                productsData = (ProductsData) message;
            } else {
                throw new IllegalArgumentException("Unexpected message type: " + message.getClass());
            }

            logger.info("Processing products message: {} products received, traceId: {}",
                    productsData.products().size(), traceId);

            // Process products data
            processProductsData(productsData, traceId);

        } catch (Exception e) {
            logger.error("Error processing products message, traceId: {}, error: {}", traceId, e.getMessage(), e);
        }
    }

    private void handleGenericMessage(Object message, String traceId) {
        logger.info("Processing generic message of type: {}, traceId: {}",
                message.getClass().getSimpleName(), traceId);

        // Try to determine type from message content
        if (message instanceof Map) {
            Map<String, Object> messageMap = (Map<String, Object>) message;

            if (messageMap.containsKey("inventory")) {
                handleInventoryMessage(message, traceId);
            } else if (messageMap.containsKey("products")) {
                handleProductMessage(message, traceId);
            } else {
                logger.warn("Could not determine message type from content, traceId: {}", traceId);
            }
        }
    }

    private void processInventoryData(InventoryData inventoryData, String traceId) {
        logger.info("Processing inventory data with {} articles, traceId: {}",
                inventoryData.inventory().size(), traceId);

        // Here you would typically update inventory in database, call other services, etc.
        inventoryData.inventory().forEach(item ->
                logger.debug("Article {}: {} (stock: {}), traceId: {}",
                        item.artId(), item.name(), item.stock(), traceId));

        logger.info("Successfully processed inventory data, traceId: {}", traceId);
    }

    private void processProductsData(ProductsData productsData, String traceId) {
        logger.info("Processing products data with {} products, traceId: {}",
                productsData.products().size(), traceId);

        // Here you would typically save products to database, call other services, etc.
        productsData.products().forEach(product ->
                logger.debug("Product: {} with {} articles, traceId: {}",
                        product.name(), product.containArticles().size(), traceId));

        logger.info("Successfully processed products data, traceId: {}", traceId);
    }
}
