package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.InventoryItem;
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

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private KafkaProducerService kafkaProducerService;

    @Test
    void uploadInventory_WithValidFile_ShouldReturnSuccess() throws Exception {
        // Arrange
        InventoryData testData = new InventoryData(List.of(
            new InventoryItem("1", "leg", "12"),
            new InventoryItem("2", "screw", "17")
        ));

        String jsonContent = objectMapper.writeValueAsString(testData);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "inventory.json",
            MediaType.APPLICATION_JSON_VALUE,
            jsonContent.getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/inventory/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(content().string("Inventory uploaded successfully. 2 articles processed."));

        verify(kafkaProducerService).sendMessage(anyString());
    }

    @Test
    void uploadInventory_WithEmptyFile_ShouldReturnBadRequest() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.json",
            MediaType.APPLICATION_JSON_VALUE,
            new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/inventory/upload").file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("File is empty"));
    }

    @Test
    void uploadInventoryData_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        InventoryData testData = new InventoryData(List.of(
            new InventoryItem("1", "leg", "12"),
            new InventoryItem("2", "screw", "17")
        ));

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testData)))
            .andExpect(status().isOk())
            .andExpect(content().string("Inventory data processed successfully. 2 articles received."));

        verify(kafkaProducerService).sendMessage(anyString());
    }

    @Test
    void uploadInventoryData_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}"))
            .andExpect(status().isBadRequest());
    }
}
