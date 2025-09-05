package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public void publishInventoryData(InventoryData inventoryData) {
        kafkaProducerService.sendInventoryData(inventoryData);
    }

    public InventoryData proceedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("File is empty");
        }
        try {
            InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);
            publishInventoryData(inventoryData);
            return inventoryData;
        } catch (Exception e) {
            throw new FileProcessingException("Failed to process products file", e);
        }
    }
}
