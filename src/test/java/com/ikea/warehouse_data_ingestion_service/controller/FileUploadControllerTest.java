package com.ikea.warehouse_data_ingestion_service.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private MockMultipartFile loadClasspathFile(String partName, String classpathLocation) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classpathLocation)) {
            if (is == null) throw new IOException("Resource not found: " + classpathLocation);
            return new MockMultipartFile(partName, partName + ".json", MediaType.APPLICATION_JSON_VALUE, is.readAllBytes());
        }
    }

    @Test
    @DisplayName("Should ingest both inventory and products files successfully")
    void ingestBothFiles() throws Exception {
        MockMultipartFile inventory = loadClasspathFile("inventory", "/static/inventory.json");
        MockMultipartFile products = loadClasspathFile("products", "/static/products.json");

        mockMvc.perform(multipart("/api/files/ingest")
                        .file(inventory)
                        .file(products))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryCount").value(4))
                .andExpect(jsonPath("$.productCount").value(2))
                .andExpect(jsonPath("$.status").value("INGESTED"));
    }

    @Test
    @DisplayName("Should ingest only inventory file when products missing")
    void ingestOnlyInventory() throws Exception {
        MockMultipartFile inventory = loadClasspathFile("inventory", "/static/inventory.json");

        mockMvc.perform(multipart("/api/files/ingest")
                        .file(inventory))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryCount").value(4))
                .andExpect(jsonPath("$.productCount").value(0));
    }

    @Test
    @DisplayName("Should return 400 when no files are provided")
    void ingestNoFiles() throws Exception {
        mockMvc.perform(multipart("/api/files/ingest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("At least one file (inventory or products) must be provided"));
    }
}

