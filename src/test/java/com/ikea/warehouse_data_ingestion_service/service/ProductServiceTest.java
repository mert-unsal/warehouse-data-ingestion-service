package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.Product;
import com.ikea.warehouse_data_ingestion_service.data.dto.ProductsData;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class ProductServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // set topic via reflection since it's injected by @Value
        TestUtils.setField(productService, "product-topic");
    }

    @Test
    void proceedFile_whenFileIsNull_shouldThrow() {
        assertThrows(FileProcessingException.class, () -> productService.proceedFile(null, Instant.now()));
    }

    @Test
    void proceedFile_shouldParseAndSendBatch() throws Exception {
        String json = "{\n  \"products\": [\n    {\n      \"name\": \"table\",\n      \"contain_articles\": []\n    },\n    {\n      \"name\": \"chair\",\n      \"contain_articles\": []\n    }\n  ]\n}";
        MockMultipartFile file = new MockMultipartFile("file", "products.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        ProductsData data = new ProductsData(List.of(
                new Product("table", List.of()),
                new Product("chair", List.of())
        ));
        // mock mapper
        org.mockito.Mockito.when(objectMapper.readValue(any(java.io.InputStream.class), eq(ProductsData.class)))
                .thenReturn(data);

        productService.proceedFile(file, Instant.parse("2025-01-01T00:00:00Z"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaProducerService).sendBatch(eq("product-topic"), mapCaptor.capture());

        Map<String, Object> sentMap = mapCaptor.getValue();
        assertEquals(2, sentMap.size());
        assertTrue(sentMap.containsKey("table"));
        assertTrue(sentMap.containsKey("chair"));
    }
}
