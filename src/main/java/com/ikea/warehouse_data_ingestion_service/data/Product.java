package com.ikea.warehouse_data_ingestion_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Product definition with required articles")
public record Product(
    @Schema(description = "Product name", example = "Dining Chair")
    String name,

    @JsonProperty("contain_articles")
    @Schema(description = "List of articles required to build this product")
    List<ArticleAmount> containArticles
) {}
