package com.ikea.warehouse_data_ingestion_service.config;

import com.ikea.warehouse_data_ingestion_service.service.TraceContext;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaProducerTracingInterceptor implements ProducerInterceptor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerTracingInterceptor.class);

    @Override
    public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
        try {
            // Get current trace ID from MDC
            String traceId = TraceContext.getCurrentTraceIdStatic();

            if (traceId != null && !traceId.trim().isEmpty()) {
                // Add trace ID to Kafka message headers
                Headers headers = record.headers();
                headers.add(TraceContext.TRACE_ID_HEADER, traceId.getBytes(StandardCharsets.UTF_8));

                // Add timestamp for message tracking
                headers.add("X-Timestamp", String.valueOf(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));

                logger.debug("Added trace ID to Kafka message - Topic: {}, TraceId: {}, Key: {}",
                           record.topic(), traceId, record.key());
            } else {
                logger.warn("No trace ID found in MDC for Kafka message - Topic: {}, Key: {}",
                          record.topic(), record.key());
            }
        } catch (Exception e) {
            logger.error("Error adding trace ID to Kafka message", e);
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            String traceId = TraceContext.getCurrentTraceIdStatic();
            logger.error("Kafka message send failed - Topic: {}, Partition: {}, Offset: {}, TraceId: {}",
                       metadata != null ? metadata.topic() : "unknown",
                       metadata != null ? metadata.partition() : -1,
                       metadata != null ? metadata.offset() : -1,
                       traceId, exception);
        } else if (metadata != null) {
            String traceId = TraceContext.getCurrentTraceIdStatic();
            logger.debug("Kafka message sent successfully - Topic: {}, Partition: {}, Offset: {}, TraceId: {}",
                       metadata.topic(), metadata.partition(), metadata.offset(), traceId);
        }
    }

    @Override
    public void close() {
        logger.info("Kafka producer tracing interceptor closed");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        logger.info("Kafka producer tracing interceptor configured");
    }
}
