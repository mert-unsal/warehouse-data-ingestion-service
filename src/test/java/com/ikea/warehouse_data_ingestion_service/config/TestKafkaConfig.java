package com.ikea.warehouse_data_ingestion_service.config;

import com.ikea.warehouse_data_ingestion_service.service.KafkaProducerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Test configuration to provide mocked Kafka dependencies when running tests.
 * This avoids the need for a real Kafka instance during testing.
 */
@Configuration
@Profile("test")
public class TestKafkaConfig {

    @Bean
    public KafkaProducerService kafkaProducerService() {
        return mock(KafkaProducerService.class);
    }
}
