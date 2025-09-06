package com.ikea.warehouse_data_ingestion_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryData;
import com.ikea.warehouse_data_ingestion_service.data.dto.InventoryItem;
import com.ikea.warehouse_data_ingestion_service.exception.FileProcessingException;
import com.ikea.warehouse_data_ingestion_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@ActiveProfiles("test")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

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
        // Mock service to return testData
        when(inventoryService.proceedFile(any(MultipartFile.class))).thenReturn(testData);
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/inventory/upload").file(file))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Inventory uploaded successfully. 2 articles processed.")));
        verify(inventoryService).proceedFile(any(MultipartFile.class));
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
        when(inventoryService.proceedFile(any(MultipartFile.class)))
            .thenThrow(new FileProcessingException("File is empty", FILE_PROCESSING_ERROR));
        // Act & Assert
        mockMvc.perform(multipart("/api/v1/inventory/upload").file(emptyFile))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("File is empty"))
            .andExpect(jsonPath("$.error").value(FILE_PROCESSING_ERROR));
    }

    @Test
    void uploadInventoryData_WithValidData_ShouldReturnSuccess() throws Exception {
        // Arrange
        List<InventoryItem> items = List.of(
            new InventoryItem("1", "leg", "12"),
            new InventoryItem("2", "screw", "17")
        );
        java.util.Map<String, Object> requestPayload = java.util.Map.of("inventory", items);

        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestPayload)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Inventory data processed successfully. 2 articles received.")));

        verify(inventoryService).publishInventoryData(any(InventoryData.class));
    }

    @Test
    void uploadInventoryData_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/inventory/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid inventory data provided"))
            .andExpect(jsonPath("$.error").value(FILE_PROCESSING_ERROR));
    }
}
