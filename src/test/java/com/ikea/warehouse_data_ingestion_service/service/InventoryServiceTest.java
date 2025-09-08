package com.ikea.warehouse_data_ingestion_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryItem;
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

class InventoryServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TestUtils.setField(inventoryService, "inventory-topic");
    }

    @Test
    void proceedFile_whenFileIsNull_shouldThrow() {
        assertThrows(FileProcessingException.class, () -> inventoryService.proceedFile(null, Instant.now()));
    }

    @Test
    void proceedFile_shouldParseAndSendBatch() throws Exception {
        String json = "{\n  \"inventory\": [\n    {\n      \"art_id\": \"1\",\n      \"name\": \"leg\",\n      \"stock\": 5\n    },\n    {\n      \"art_id\": \"2\",\n      \"name\": \"screw\",\n      \"stock\": 10\n    }\n  ]\n}";
        MockMultipartFile file = new MockMultipartFile("file", "inventory.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        InventoryData data = new InventoryData(List.of(
                new InventoryItem("1", "leg", "5"),
                new InventoryItem("2", "screw", "10")
        ));
        org.mockito.Mockito.when(objectMapper.readValue(any(java.io.InputStream.class), eq(InventoryData.class)))
                .thenReturn(data);

        inventoryService.proceedFile(file, Instant.parse("2025-01-01T00:00:00Z"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaProducerService).sendBatch(eq("inventory-topic"), mapCaptor.capture());

        Map<String, Object> sentMap = mapCaptor.getValue();
        assertEquals(2, sentMap.size());
        assertTrue(sentMap.containsKey("1"));
        assertTrue(sentMap.containsKey("2"));
    }
}
