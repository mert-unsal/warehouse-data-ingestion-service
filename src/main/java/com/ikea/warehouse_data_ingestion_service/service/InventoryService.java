package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryItem;
import com.ikea.warehouse_data_ingestion_service.data.event.InventoryUpdateEvent;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.stream.Collectors;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorTypes.FILE_PROCESSING_ERROR;


@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    @Value("${app.kafka.topics.inventory}")
    private String productTopic;

    public void proceedFile(MultipartFile file, Instant fileCreatedAt) throws IOException {
        if (ObjectUtils.isEmpty(file)) {
            throw new FileProcessingException(ErrorMessages.FILE_EMPTY, FILE_PROCESSING_ERROR);
        }

        InventoryData inventoryData = objectMapper.readValue(file.getInputStream(), InventoryData.class);

        kafkaProducerService.sendBatch(productTopic, inventoryData.inventory()
                .stream()
                .collect(Collectors.toMap(InventoryItem::artId, inventoryItem -> InventoryUpdateEvent.builder()
                        .artId(inventoryItem.artId())
                        .name(inventoryItem.name())
                        .stock(inventoryItem.stock())
                        .fileCreatedAt(fileCreatedAt)
                        .build())));

    }
}
