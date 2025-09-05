package com.ikea.warehouse_data_ingestion_service.util;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OPERATION_MDC_KEY = "operation";

    // Private constructor to prevent instantiation
    private TraceContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
    }

    public static void setOperation(String operation) {
        if (operation != null && !operation.trim().isEmpty()) {
            MDC.put(OPERATION_MDC_KEY, operation);
        }
    }

    public static String getCurrentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    public static String getCurrentOperation() {
        return MDC.get(OPERATION_MDC_KEY);
    }

    public static void clearTrace() {
        MDC.remove(TRACE_ID_MDC_KEY);
        MDC.remove(OPERATION_MDC_KEY);
    }

    public static void clearAll() {
        MDC.clear();
    }
}
