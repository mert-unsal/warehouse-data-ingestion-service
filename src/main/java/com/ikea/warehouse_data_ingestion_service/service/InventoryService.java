package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
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
        String key = String.valueOf(inventoryData.hashCode());
        kafkaProducerService.sendInventoryUpdate(key, inventoryData);
        log.info("Published inventory data to Kafka with key '{}'", key);
    }

    public InventoryData proceedFile(MultipartFile file) throws com.fasterxml.jackson.core.JsonProcessingException {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException(ErrorMessages.FILE_EMPTY);
        }
        try {
            InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);
            publishInventoryData(inventoryData);
            return inventoryData;
        } catch (Exception e) {
            throw new FileProcessingException(ErrorMessages.FAILED_TO_PROCESS_INVENTORY_FILE, e);
        }
    }
}
