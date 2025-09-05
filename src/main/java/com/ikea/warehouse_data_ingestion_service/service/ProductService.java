package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;

    public void publishProductData(ProductsData productsData) {
        kafkaProducerService.sendProductData(productsData);
    }

    public void proceedFile(MultipartFile file) {
        try {
            ProductsData productsData = objectMapper.readValue(file.getInputStream(), ProductsData.class);
            publishProductData(productsData);
        } catch (Exception e) {
            throw new FileProcessingException("Failed to process products file", e);
        }
    }
}
