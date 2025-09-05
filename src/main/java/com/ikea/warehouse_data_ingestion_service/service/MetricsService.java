package com.ikea.warehouse_data_ingestion_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final Counter productsUploadCounter;
    private final Counter inventoryUploadCounter;
    private final Counter kafkaMessagesCounter;
    private final Timer uploadProcessingTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.productsUploadCounter = Counter.builder("warehouse.products.uploads.total")
                .description("Total number of product uploads")
                .register(meterRegistry);

        this.inventoryUploadCounter = Counter.builder("warehouse.inventory.uploads.total")
                .description("Total number of inventory uploads")
                .register(meterRegistry);

        this.kafkaMessagesCounter = Counter.builder("warehouse.kafka.messages.sent.total")
                .description("Total number of messages sent to Kafka")
                .register(meterRegistry);

        this.uploadProcessingTimer = Timer.builder("warehouse.upload.processing.time")
                .description("Time taken to process uploads")
                .register(meterRegistry);
    }

    public void recordProductUpload(int productCount) {
        productsUploadCounter.increment();
        logger.info("Product upload recorded - count: {}, total uploads: {}",
                   productCount, productsUploadCounter.count());
    }

    public void recordInventoryUpload(int articleCount) {
        inventoryUploadCounter.increment();
        logger.info("Inventory upload recorded - count: {}, total uploads: {}",
                   articleCount, inventoryUploadCounter.count());
    }

    public void recordKafkaMessage() {
        kafkaMessagesCounter.increment();
        logger.debug("Kafka message sent - total messages: {}", kafkaMessagesCounter.count());
    }

    public Timer.Sample startUploadTimer() {
        return Timer.start();
    }

    public void stopUploadTimer(Timer.Sample sample) {
        sample.stop(uploadProcessingTimer);
    }
}
