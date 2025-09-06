package com.ikea.warehouse_data_ingestion_service.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should handle JSON processing errors with standardized error response")
    void shouldHandleJsonProcessingError() throws Exception {
        MockMultipartFile invalidJsonFile = new MockMultipartFile(
                "file",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{ invalid json".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/inventory/upload")
                        .file(invalidJsonFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Failed to process inventory file")));
    }

    @Test
    @DisplayName("Should handle empty file uploads gracefully")
    void shouldHandleEmptyFileUpload() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/inventory/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    @DisplayName("Should handle invalid JSON data payload")
    void shouldHandleInvalidJsonPayload() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invalid\": \"structure\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid inventory data provided")));
    }

    @Test
    @DisplayName("Should handle successful inventory processing")
    void shouldHandleSuccessfulProcessing() throws Exception {
        String validInventoryJson = "{\"inventory\":[{\"art_id\":\"1\",\"name\":\"leg\",\"stock\":\"12\"}]}";
        MockMultipartFile validFile = new MockMultipartFile(
                "file",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                validInventoryJson.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/inventory/upload")
                        .file(validFile))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inventory uploaded successfully")));
    }
}
