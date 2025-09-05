package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.Product;
import com.ikea.warehouse_data_ingestion_service.data.ProductsData;
import com.ikea.warehouse_data_ingestion_service.data.ArticleAmount;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @Test
    void uploadProducts_WithValidFile_ShouldReturnSuccess() throws Exception {
        // Arrange
        ProductsData testData = new ProductsData(List.of(
            new Product("Dining Chair", List.of(
                new ArticleAmount("1", "1"),
                new ArticleAmount("2", "8")
            ))
        ));

        String jsonContent = objectMapper.writeValueAsString(testData);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "products.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(content().string("Products uploaded successfully. 1 products processed."));

        verify(kafkaProducerService).sendMessage(anyString());
    }

    @Test
    void uploadProducts_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.json",
            MediaType.APPLICATION_JSON_VALUE,
            new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/products/upload").file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("File is empty"));
    }

    @Test
    void uploadProductsData_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        ProductsData testData = new ProductsData(List.of(
            new Product("Dining Chair", List.of(
                new ArticleAmount("1", "1"),
                new ArticleAmount("2", "8")
            ))
        ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/products/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testData)))
            .andExpect(status().isOk())
            .andExpect(content().string("Products data processed successfully. 1 products received."));

        verify(kafkaProducerService).sendMessage(anyString());
    }

    @Test
    void uploadProductsData_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/products/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}"))
            .andExpect(status().isBadRequest());
    }
}
