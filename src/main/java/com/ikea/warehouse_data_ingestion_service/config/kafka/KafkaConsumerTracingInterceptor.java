package com.ikea.warehouse_data_ingestion_service.config.kafka;

import com.ikea.warehouse_data_ingestion_service.util.TraceContext;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class KafkaConsumerTracingInterceptor implements ConsumerInterceptor<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerTracingInterceptor.class);

    @Override
    public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
        for (ConsumerRecord<String, String> record : records) {
            try {
                // Extract trace ID from Kafka message headers
                String traceId = null;
                Header traceHeader = record.headers().lastHeader(TraceContext.TRACE_ID_HEADER);

                if (traceHeader != null) {
                    traceId = new String(traceHeader.value(), StandardCharsets.UTF_8);
                    logger.debug("Extracted trace ID from Kafka message - Topic: {}, TraceId: {}, Key: {}",
                               record.topic(), traceId, record.key());
                } else {
                    // Generate new trace ID if not found in message headers
                    traceId = TraceContext.getCurrentTraceId();
                    logger.warn("No trace ID found in Kafka message, generated new one - Topic: {}, TraceId: {}, Key: {}",
                              record.topic(), traceId, record.key());
                }

                // Set trace context for this thread
                String operation = String.format("KAFKA_CONSUME_%s", record.topic().toUpperCase());
                MDC.put(TraceContext.TRACE_ID_MDC_KEY, traceId);

                // Set operation context
                MDC.put(TraceContext.OPERATION_MDC_KEY, operation);

                // Log message processing with trace context
                logger.info("Processing Kafka message - Topic: {}, Partition: {}, Offset: {}, TraceId: {}, Operation: {}",
                           record.topic(), record.partition(), record.offset(), traceId, operation);

                // Extract timestamp if available
                Header timestampHeader = record.headers().lastHeader("X-Timestamp");
                if (timestampHeader != null) {
                    String timestamp = new String(timestampHeader.value(), StandardCharsets.UTF_8);
                    long messageTime = Long.parseLong(timestamp);
                    long processingDelay = System.currentTimeMillis() - messageTime;
                    logger.debug("Message processing delay: {}ms - TraceId: {}", processingDelay, traceId);
                }

            } catch (Exception e) {
                logger.error("Error processing trace ID from Kafka message - Topic: {}, Key: {}",
                           record.topic(), record.key(), e);
            }
        }

        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        String traceId = TraceContext.getCurrentTraceId();
        logger.debug("Kafka consumer commit completed - TraceId: {}, Offsets: {}", traceId, offsets.size());
    }

    @Override
    public void close() {
        logger.info("Kafka consumer tracing interceptor closed");
    }

    @Override
    public void configure(Map<String, ?> configs) {
        logger.info("Kafka consumer tracing interceptor configured");
    }
}
