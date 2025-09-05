package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.data.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileIngestionService {

    private final ObjectMapper objectMapper;

    public FileIngestionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> ingest(MultipartFile inventoryFile, MultipartFile productsFile) throws IOException {
        Map<String, Object> result = new HashMap<>();

        if (inventoryFile != null && !inventoryFile.isEmpty()) {
            InventoryData inventoryData = objectMapper.readValue(inventoryFile.getInputStream(), InventoryData.class);
            result.put("inventoryCount", inventoryData.getInventory() != null ? inventoryData.getInventory().size() : 0);
        } else {
            result.put("inventoryCount", 0);
        }

        if (productsFile != null && !productsFile.isEmpty()) {
            ProductsData productsData = objectMapper.readValue(productsFile.getInputStream(), ProductsData.class);
            List<Product> products = productsData.getProducts() != null ? productsData.getProducts() : List.of();
            result.put("productCount", products.size());
        } else {
            result.put("productCount", 0);
        }

        result.put("status", "INGESTED");
        return result;
    }
}
