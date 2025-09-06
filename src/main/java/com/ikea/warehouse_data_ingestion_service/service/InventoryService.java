package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorTypes.FILE_PROCESSING_ERROR;


@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public void publishInventoryData(InventoryData inventoryData) {
        String key = String.valueOf(inventoryData.hashCode());
        var event = new InventoryUpdateEvent(
            inventoryData.inventory(),
            System.currentTimeMillis()
        );
        kafkaProducerService.sendInventoryUpdate(key, event);
        log.info("Published inventory data to Kafka with key '{}'", key);
    }

    public InventoryData proceedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException(ErrorMessages.FILE_EMPTY, FILE_PROCESSING_ERROR);
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
