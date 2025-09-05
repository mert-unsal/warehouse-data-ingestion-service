package com.ikea.warehouse_data_ingestion_service.config;

import com.ikea.warehouse_data_ingestion_service.service.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TracingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingInterceptor.class);

    private final TraceContext traceContext;

    public TracingInterceptor(TraceContext traceContext) {
        this.traceContext = traceContext;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Check if trace ID exists in request header
        String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);

        if (traceId == null || traceId.trim().isEmpty()) {
            // Generate new trace ID if not present
            traceId = traceContext.generateTraceId();
            logger.debug("Generated new trace ID: {}", traceId);
        } else {
            logger.debug("Using existing trace ID: {}", traceId);
        }

        // Set trace ID in MDC for logging
        traceContext.setTraceId(traceId);

        // Set operation context based on request
        String operation = extractOperation(request);
        traceContext.setOperation(operation);

        // Add trace ID to response header for client tracking
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        logger.info("Request started - Method: {}, URI: {}, TraceId: {}, Operation: {}",
                   request.getMethod(), request.getRequestURI(), traceId, operation);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        String traceId = traceContext.getCurrentTraceId();
        String operation = traceContext.getCurrentOperation();

        logger.info("Request completed - Status: {}, TraceId: {}, Operation: {}",
                   response.getStatus(), traceId, operation);

        if (ex != null) {
            logger.error("Request failed with exception - TraceId: {}, Operation: {}",
                        traceId, operation, ex);
        }

        // Clear MDC after request completion
        traceContext.clearAll();
    }

    private String extractOperation(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Extract operation from URI patterns
        if (uri.contains("/products/upload")) return "product-file-upload";
        if (uri.contains("/products/data")) return "product-data-ingestion";
        if (uri.contains("/inventory/upload")) return "inventory-file-upload";
        if (uri.contains("/inventory/data")) return "inventory-data-ingestion";
        if (uri.contains("/kafka/send")) return "kafka-message-send";

        // Default operation format
        return method.toLowerCase() + "-" + uri.replaceAll("/", "-").replaceAll("^-|-$", "");
    }
}
