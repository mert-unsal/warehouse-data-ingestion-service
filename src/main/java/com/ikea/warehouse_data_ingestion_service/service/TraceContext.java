package com.ikea.warehouse_data_ingestion_service.service;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TraceContext {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String OPERATION_MDC_KEY = "operation";

    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
    }

    public void setOperation(String operation) {
        if (operation != null && !operation.trim().isEmpty()) {
            MDC.put(OPERATION_MDC_KEY, operation);
        }
    }

    public String getCurrentTraceId() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = generateTraceId();
            setTraceId(traceId);
        }
        return traceId;
    }

    public String getCurrentOperation() {
        return MDC.get(OPERATION_MDC_KEY);
    }

    public void clearTrace() {
        MDC.remove(TRACE_ID_MDC_KEY);
        MDC.remove(OPERATION_MDC_KEY);
    }

    public static String getCurrentTraceIdStatic() {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            MDC.put(TRACE_ID_MDC_KEY, traceId);
        }
        return traceId;
    }

    public void clearAll() {
        MDC.clear();
    }
}
