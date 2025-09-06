package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.ArticleAmount;
import com.ikea.warehouse_data_ingestion_service.data.dto.Product;
import com.ikea.warehouse_data_ingestion_service.data.dto.ProductsData;
import com.ikea.warehouse_data_ingestion_service.exception.GlobalExceptionHandler;
import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import com.ikea.warehouse_data_ingestion_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.ikea.warehouse_data_ingestion_service.util.ErrorTypes.FILE_PROCESSING_ERROR;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private ProductService productService;

    @TestConfiguration
    static class MockServiceConfig {
        @Bean
        public KafkaProducerService kafkaProducerService() {
            return Mockito.mock(KafkaProducerService.class);
        }
        @Bean
        public ProductService productService() {
            return Mockito.mock(ProductService.class);
        }
    }

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
            .andExpect(content().string(containsString("Products uploaded successfully.")));

        verify(productService).proceedFile(any(MultipartFile.class));
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
            .andExpect(jsonPath("$.message").value("File is empty"))
            .andExpect(jsonPath("$.error").value(FILE_PROCESSING_ERROR));
    }

    @Test
    void uploadProductsData_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        List<Product> products = List.of(
            new Product("Dining Chair", List.of(
                new ArticleAmount("1", "1"),
                new ArticleAmount("2", "8")
            ))
        );
        java.util.Map<String, Object> requestPayload = java.util.Map.of("products", products);

        // Act & Assert
        mockMvc.perform(post("/api/v1/products/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestPayload)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Products data processed successfully. 1 products received.")));

        verify(productService).publishProductData(any(ProductsData.class));
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
