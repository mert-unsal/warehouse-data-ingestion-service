package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.Product;
import com.ikea.warehouse_data_ingestion_service.data.dto.ProductsData;
import com.ikea.warehouse_data_ingestion_service.data.event.ProductUpdateEvent;
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

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductService {
    private final ObjectMapper objectMapper;
    private final KafkaProducerService kafkaProducerService;
    
    @Value("${app.kafka.topics.product}")
    private String productTopic;

    public void proceedFile(MultipartFile file, Instant fileCreatedAt) throws IOException {
        if (ObjectUtils.isEmpty(file)) {
            throw new FileProcessingException(ErrorMessages.FILE_EMPTY, FILE_PROCESSING_ERROR);
        }
        ProductsData productsData = objectMapper.readValue(file.getInputStream(), ProductsData.class);
        kafkaProducerService.sendBatch(productTopic, productsData.products()
                .stream()
                .collect(Collectors.toMap(Product::name, product -> ProductUpdateEvent.builder()
                        .name(product.name())
                        .containArticles(product.containArticles())
                        .fileCreatedAt(fileCreatedAt)
                        .build())));
    }
}
