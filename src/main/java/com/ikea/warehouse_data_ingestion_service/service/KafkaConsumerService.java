package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private final ObjectMapper objectMapper;

    public KafkaConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic}", groupId = "warehouse-group")
    public void listen(String message) {
        try {
            // Process different message types based on prefix
            if (message.startsWith("PRODUCT_")) {
                handleProductMessage(message);
            } else if (message.startsWith("INVENTORY_")) {
                handleInventoryMessage(message);
            } else {
                System.out.println("Received unknown message type: " + message);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private void handleProductMessage(String message) {
        try {
            String jsonData = extractJsonFromMessage(message);
            ProductsData productsData = objectMapper.readValue(jsonData, ProductsData.class);
            System.out.println("Processing products message: " + productsData.products().size() + " products received");

            // Here you would typically save to database, call other services, etc.
            // For now, just log the products
            productsData.products().forEach(product ->
                System.out.println("Product: " + product.name() + " with " + product.containArticles().size() + " articles"));

        } catch (Exception e) {
            System.err.println("Error processing product message: " + e.getMessage());
        }
    }

    private void handleInventoryMessage(String message) {
        try {
            String jsonData = extractJsonFromMessage(message);
            InventoryData inventoryData = objectMapper.readValue(jsonData, InventoryData.class);
            System.out.println("Processing inventory message: " + inventoryData.inventory().size() + " articles received");

            // Here you would typically update inventory in database, etc.
            // For now, just log the inventory items
            inventoryData.inventory().forEach(item ->
                System.out.println("Article " + item.artId() + ": " + item.name() + " (stock: " + item.stock() + ")"));

        } catch (Exception e) {
            System.err.println("Error processing inventory message: " + e.getMessage());
        }
    }

    private String extractJsonFromMessage(String message) {
        // Extract JSON from messages like "PRODUCT_UPLOAD: {json}" or "INVENTORY_DATA: {json}"
        int colonIndex = message.indexOf(": ");
        return colonIndex != -1 ? message.substring(colonIndex + 2) : message;
    }
}
