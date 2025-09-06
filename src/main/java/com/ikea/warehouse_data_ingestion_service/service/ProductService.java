package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.ProductsData;
import com.ikea.warehouse_data_ingestion_service.data.event.ProductUpdateEvent;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.util.ErrorMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorTypes.FILE_PROCESSING_ERROR;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public void publishProductData(ProductsData productsData) {
        String key = String.valueOf(productsData.hashCode());
        var event = new ProductUpdateEvent(
            productsData.products(),
            System.currentTimeMillis()
        );
        kafkaProducerService.sendProductUpdate(key, event);
        log.info("Published product data to Kafka with key '{}'", key);
    }

    public void proceedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException(ErrorMessages.FILE_EMPTY, FILE_PROCESSING_ERROR);
        }
        try {
            ProductsData productsData = objectMapper.readValue(file.getInputStream(), ProductsData.class);
            publishProductData(productsData);
        } catch (Exception e) {
            throw new FileProcessingException(ErrorMessages.FAILED_TO_PROCESS_PRODUCTS_FILE, e);
        }
    }
}
