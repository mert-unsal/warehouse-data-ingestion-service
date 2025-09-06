package com.ikea.warehouse_data_ingestion_service.data.event;

import com.ikea.warehouse_data_ingestion_service.data.dto.ArticleAmount;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record ProductUpdateEvent(
        String name,
        List<ArticleAmount> containArticles,
        Instant fileCreatedAt
) {}
