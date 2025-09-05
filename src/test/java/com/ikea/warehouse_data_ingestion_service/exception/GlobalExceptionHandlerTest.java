package com.ikea.warehouse_data_ingestion_service.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should handle JSON processing errors with standardized error response")
    void shouldHandleJsonProcessingError() throws Exception {
        MockMultipartFile invalidJsonFile = new MockMultipartFile(
                "inventory",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{ invalid json".getBytes()
        );

        mockMvc.perform(multipart("/api/files/ingest")
                        .file(invalidJsonFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_JSON_FORMAT"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/files/ingest"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle empty file uploads gracefully")
    void shouldHandleEmptyFileUpload() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "inventory",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/files/ingest")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("At least one file (inventory or products) must be provided"));
    }

    @Test
    @DisplayName("Should handle unsupported media type with standardized error response")
    void shouldHandleUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/files/ingest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Content type not supported. Please use multipart/form-data for file uploads."))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.path").value("/api/files/ingest"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should handle graceful processing of files with missing expected structure")
    void shouldHandleGracefulProcessing() throws Exception {
        MockMultipartFile malformedFile = new MockMultipartFile(
                "inventory",
                "inventory.json",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"wrong_structure\": []}".getBytes()
        );

        mockMvc.perform(multipart("/api/files/ingest")
                        .file(malformedFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.inventoryCount").value(0))
                .andExpect(jsonPath("$.status").value("INGESTED"));
    }
}
