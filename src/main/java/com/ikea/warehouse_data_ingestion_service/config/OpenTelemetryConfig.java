package com.ikea.warehouse_data_ingestion_service.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${spring.application.name}")
    private String serviceName;

    @Value("${opentelemetry.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, "1.0.0",
                        ResourceAttributes.DEPLOYMENT_ENVIRONMENT, getEnvironment()
                )));

        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(
                                        io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(
                                                JaegerGrpcSpanExporter.builder()
                                                        .setEndpoint(jaegerEndpoint)
                                                        .build())
                                                .build())
                                .setResource(resource)
                                .build())
                .setMeterProvider(
                        SdkMeterProvider.builder()
                                .setResource(resource)
                                .build())
                .buildAndRegisterGlobal();
    }

    @Bean
    public Meter meter(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeter(serviceName);
    }

    @Bean
    public io.opentelemetry.api.trace.Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName);
    }

    private String getEnvironment() {
        return System.getenv().getOrDefault("DEPLOYMENT_ENV", "development");
    }
}
