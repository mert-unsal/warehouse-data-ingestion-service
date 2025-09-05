package com.ikea.warehouse_data_ingestion_service.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;
    private final AtomicInteger currentProcessingCount = new AtomicInteger(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register gauge for current processing count using correct API
        meterRegistry.gauge("warehouse.processing.active",
                           Tags.of("description", "Number of currently active processing operations"),
                           currentProcessingCount,
                           AtomicInteger::get);
    }

    public void recordSuccessfulUpload(String type, int itemCount, long fileSizeBytes) {
        Counter.builder("warehouse.uploads.total")
                .description("Total number of successful uploads")
                .tags("type", type, "outcome", "success", "size_category", categorizeSizeBytes(fileSizeBytes))
                .register(meterRegistry)
                .increment();

        Counter.builder("warehouse.items.processed")
                .description("Total number of items processed")
                .tags("type", type)
                .register(meterRegistry)
                .increment(itemCount);

        logger.debug("Recorded successful {} upload - {} items, {} bytes", type, itemCount, fileSizeBytes);
    }

    public void recordFailedUpload(String type, String errorType) {
        Counter.builder("warehouse.uploads.total")
                .description("Total number of failed uploads")
                .tags("type", type, "outcome", "error", "error_type", errorType)
                .register(meterRegistry)
                .increment();

        logger.warn("Recorded failed {} upload - error type: {}", type, errorType);
    }

    public void recordKafkaMessage(String messageType) {
        Timer.Sample kafkaSample = Timer.start(meterRegistry);
        try {
            Counter.builder("warehouse.kafka.messages.sent")
                    .description("Total number of messages sent to Kafka")
                    .tags("message_type", messageType)
                    .register(meterRegistry)
                    .increment();
            logger.debug("Recorded Kafka message - type: {}", messageType);
        } finally {
            kafkaSample.stop(Timer.builder("warehouse.kafka.publish.duration")
                    .description("Time taken to publish messages to Kafka")
                    .register(meterRegistry));
        }
    }

    // Timer management with proper resource tracking
    public Timer.Sample startFileUploadTimer() {
        currentProcessingCount.incrementAndGet();
        return Timer.start(meterRegistry);
    }

    public void stopFileUploadTimer(Timer.Sample sample) {
        try {
            sample.stop(Timer.builder("warehouse.upload.duration")
                    .description("Time taken to upload and parse files")
                    .register(meterRegistry));
        } finally {
            currentProcessingCount.decrementAndGet();
        }
    }

    public Timer.Sample startDataProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopDataProcessingTimer(Timer.Sample sample) {
        sample.stop(Timer.builder("warehouse.processing.duration")
                .description("Time taken to process data")
                .register(meterRegistry));
    }

    // Business metrics
    public void recordProcessingMetrics(String operation, String type, int itemCount, long durationMs) {
        Counter.builder("warehouse.operations.total")
                .description("Total warehouse operations")
                .tags("operation", operation, "type", type, "status", "completed")
                .register(meterRegistry)
                .increment();

        Timer.builder("warehouse.operation.duration")
                .description("Duration of warehouse operations")
                .tags("operation", operation, "type", type)
                .register(meterRegistry)
                .record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);

        Counter.builder("warehouse.items.total")
                .description("Total items processed by operation")
                .tags("operation", operation, "type", type)
                .register(meterRegistry)
                .increment(itemCount);
    }

    // Helper methods
    private String categorizeSizeBytes(long bytes) {
        if (bytes < 1024) return "small";
        if (bytes < 1024 * 1024) return "medium";
        if (bytes < 10 * 1024 * 1024) return "large";
        return "xlarge";
    }
}
