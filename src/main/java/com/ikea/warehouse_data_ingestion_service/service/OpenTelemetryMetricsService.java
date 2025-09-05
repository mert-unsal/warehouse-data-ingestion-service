package com.ikea.warehouse_data_ingestion_service.service;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OpenTelemetryMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryMetricsService.class);

    // Semantic attribute keys following OpenTelemetry conventions
    private static final AttributeKey<String> OPERATION_TYPE = AttributeKey.stringKey("operation.type");
    private static final AttributeKey<String> DATA_TYPE = AttributeKey.stringKey("data.type");
    private static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final AttributeKey<Long> ITEM_COUNT = AttributeKey.longKey("items.count");
    private static final AttributeKey<Long> FILE_SIZE = AttributeKey.longKey("file.size.bytes");
    private static final AttributeKey<String> FILE_SIZE_CATEGORY = AttributeKey.stringKey("file.size.category");

    // OpenTelemetry instruments
    private final LongCounter uploadCounter;
    private final LongCounter itemsProcessedCounter;
    private final LongCounter kafkaMessagesCounter;
    private final DoubleHistogram uploadDurationHistogram;
    private final DoubleHistogram processingDurationHistogram;
    private final LongUpDownCounter activeOperationsGauge;

    // Tracer for creating spans
    private final Tracer tracer;

    // Active operations tracker
    private final AtomicLong activeOperations = new AtomicLong(0);

    public OpenTelemetryMetricsService(Meter meter, Tracer tracer) {
        this.tracer = tracer;

        // Initialize counters with semantic naming
        this.uploadCounter = meter
                .counterBuilder("warehouse.uploads.total")
                .setDescription("Total number of file uploads processed")
                .setUnit("1")
                .build();

        this.itemsProcessedCounter = meter
                .counterBuilder("warehouse.items.processed.total")
                .setDescription("Total number of inventory/product items processed")
                .setUnit("1")
                .build();

        this.kafkaMessagesCounter = meter
                .counterBuilder("warehouse.kafka.messages.sent.total")
                .setDescription("Total number of messages sent to Kafka")
                .setUnit("1")
                .build();

        // Initialize histograms for duration metrics
        this.uploadDurationHistogram = meter
                .histogramBuilder("warehouse.upload.duration")
                .setDescription("Duration of file upload operations")
                .setUnit("ms")
                .build();

        this.processingDurationHistogram = meter
                .histogramBuilder("warehouse.processing.duration")
                .setDescription("Duration of data processing operations")
                .setUnit("ms")
                .build();

        // Initialize gauge for active operations
        this.activeOperationsGauge = meter
                .upDownCounterBuilder("warehouse.operations.active")
                .setDescription("Number of currently active operations")
                .setUnit("1")
                .build();

        // Register callback for the gauge to read from our atomic counter
        meter.gaugeBuilder("warehouse.operations.active.gauge")
                .setDescription("Current number of active operations")
                .buildWithCallback(measurement ->
                    measurement.record(activeOperations.get()));
    }

    /**
     * Record a successful upload operation with OpenTelemetry best practices
     */
    public void recordSuccessfulUpload(String dataType, int itemCount, long fileSizeBytes) {
        Attributes attributes = Attributes.of(
                DATA_TYPE, dataType,
                OUTCOME, "success",
                ITEM_COUNT, (long) itemCount,
                FILE_SIZE, fileSizeBytes,
                FILE_SIZE_CATEGORY, categorizeFileSize(fileSizeBytes)
        );

        // Record metrics
        uploadCounter.add(1, attributes);
        itemsProcessedCounter.add(itemCount, Attributes.of(DATA_TYPE, dataType));

        // Add span event if we're in a trace context
        Span currentSpan = Span.current();
        if (currentSpan.isRecording()) {
            currentSpan.addEvent("upload.success", attributes);
            currentSpan.setStatus(StatusCode.OK);
        }

        logger.debug("Recorded successful {} upload - {} items, {} bytes", dataType, itemCount, fileSizeBytes);
    }

    /**
     * Record a failed upload operation
     */
    public void recordFailedUpload(String dataType, String errorType) {
        Attributes attributes = Attributes.of(
                DATA_TYPE, dataType,
                OUTCOME, "error",
                ERROR_TYPE, errorType
        );

        uploadCounter.add(1, attributes);

        // Add span event and mark span as error
        Span currentSpan = Span.current();
        if (currentSpan.isRecording()) {
            currentSpan.addEvent("upload.error", attributes);
            currentSpan.setStatus(StatusCode.ERROR, "Upload failed: " + errorType);
        }

        logger.warn("Recorded failed {} upload - error type: {}", dataType, errorType);
    }

    /**
     * Record Kafka message sent
     */
    public void recordKafkaMessage(String messageType) {
        Attributes attributes = Attributes.of(
                OPERATION_TYPE, messageType.toLowerCase()
        );

        kafkaMessagesCounter.add(1, attributes);

        // Add span event
        Span currentSpan = Span.current();
        if (currentSpan.isRecording()) {
            currentSpan.addEvent("kafka.message.sent", attributes);
        }

        logger.debug("Recorded Kafka message - type: {}", messageType);
    }

    /**
     * Create a timed operation context for file uploads
     */
    public TimedOperation startFileUploadOperation(String operationType, String dataType) {
        activeOperations.incrementAndGet();
        activeOperationsGauge.add(1);

        // Create a child span for the operation
        Span span = tracer.spanBuilder("warehouse.upload." + dataType)
                .setParent(Context.current())
                .setAttribute(OPERATION_TYPE, operationType)
                .setAttribute(DATA_TYPE, dataType)
                .startSpan();

        return new TimedOperation(span, Instant.now(), operationType, dataType);
    }

    /**
     * Create a timed operation context for data processing
     */
    public TimedOperation startProcessingOperation(String operationType, String dataType) {
        Span span = tracer.spanBuilder("warehouse.processing." + dataType)
                .setParent(Context.current())
                .setAttribute(OPERATION_TYPE, operationType)
                .setAttribute(DATA_TYPE, dataType)
                .startSpan();

        return new TimedOperation(span, Instant.now(), operationType, dataType);
    }

    /**
     * Helper class for timed operations
     */
    public class TimedOperation implements AutoCloseable {
        private final Span span;
        private final Scope scope;
        private final Instant startTime;
        private final String operationType;
        private final String dataType;

        public TimedOperation(Span span, Instant startTime, String operationType, String dataType) {
            this.span = span;
            this.scope = span.makeCurrent();
            this.startTime = startTime;
            this.operationType = operationType;
            this.dataType = dataType;
        }

        public void recordSuccess(int itemCount) {
            double durationMs = Duration.between(startTime, Instant.now()).toMillis();

            Attributes attributes = Attributes.of(
                    OPERATION_TYPE, operationType,
                    DATA_TYPE, dataType,
                    OUTCOME, "success",
                    ITEM_COUNT, (long) itemCount
            );

            if (operationType.contains("upload")) {
                uploadDurationHistogram.record(durationMs, attributes);
            } else {
                processingDurationHistogram.record(durationMs, attributes);
            }

            span.setStatus(StatusCode.OK);
            span.setAttribute(ITEM_COUNT, (long) itemCount);
        }

        public void recordError(String errorMessage) {
            double durationMs = Duration.between(startTime, Instant.now()).toMillis();

            Attributes attributes = Attributes.of(
                    OPERATION_TYPE, operationType,
                    DATA_TYPE, dataType,
                    OUTCOME, "error"
            );

            if (operationType.contains("upload")) {
                uploadDurationHistogram.record(durationMs, attributes);
            } else {
                processingDurationHistogram.record(durationMs, attributes);
            }

            span.setStatus(StatusCode.ERROR, errorMessage);
        }

        @Override
        public void close() {
            try {
                scope.close();
                span.end();
            } finally {
                if (operationType.contains("upload")) {
                    activeOperations.decrementAndGet();
                    activeOperationsGauge.add(-1);
                }
            }
        }
    }

    private String categorizeFileSize(long bytes) {
        if (bytes < 1024) return "small";
        if (bytes < 1024 * 1024) return "medium";
        if (bytes < 10 * 1024 * 1024) return "large";
        return "xlarge";
    }
}
