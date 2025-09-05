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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Check if trace ID exists in request header
        String traceId = request.getHeader(TraceContext.TRACE_ID_HEADER);

        if (traceId == null || traceId.trim().isEmpty()) {
            // Generate new trace ID if not present
            traceId = TraceContext.generateTraceId();
            logger.debug("Generated new trace ID: {}", traceId);
        } else {
            logger.debug("Using existing trace ID from header: {}", traceId);
        }

        // Set trace ID in MDC for this request thread
        TraceContext.setTraceId(traceId);

        // Set operation context based on request path and method
        String operation = String.format("%s_%s",
            request.getMethod(),
            request.getRequestURI().replaceAll("/", "_").toUpperCase());
        TraceContext.setOperation(operation);

        // Add trace ID to response header
        response.setHeader(TraceContext.TRACE_ID_HEADER, traceId);

        logger.info("Request started - Method: {}, URI: {}, TraceId: {}, Operation: {}",
                   request.getMethod(), request.getRequestURI(), traceId, operation);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        try {
            String traceId = TraceContext.getCurrentTraceId();

            if (ex != null) {
                logger.error("Request completed with error - Method: {}, URI: {}, Status: {}, TraceId: {}",
                           request.getMethod(), request.getRequestURI(), response.getStatus(), traceId, ex);
            } else {
                logger.info("Request completed successfully - Method: {}, URI: {}, Status: {}, TraceId: {}",
                           request.getMethod(), request.getRequestURI(), response.getStatus(), traceId);
            }
        } finally {
            // Clean up MDC to prevent memory leaks
            TraceContext.clearTrace();
        }
    }
}
