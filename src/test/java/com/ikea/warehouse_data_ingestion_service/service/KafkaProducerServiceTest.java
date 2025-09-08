package com.ikea.warehouse_data_ingestion_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaProducerServiceTest {

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    KafkaProducerService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void sendBatch_sendsAllMessages() {
        Map<String, Object> map = new HashMap<>();
        map.put("k1", Map.of("v", 1));
        map.put("k2", Map.of("v", 2));

        CompletableFuture<SendResult<String, Object>> ok = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(eq("topic"), any(String.class), any())).thenReturn(ok);

        assertDoesNotThrow(() -> service.sendBatch("topic", map));

        verify(kafkaTemplate, times(2)).send(eq("topic"), any(String.class), any());
    }
}
