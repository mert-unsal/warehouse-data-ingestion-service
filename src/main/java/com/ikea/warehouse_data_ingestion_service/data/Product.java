package com.ikea.warehouse_data_ingestion_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class Product {
    private String name;
    @JsonProperty("contain_articles")
    private List<ArticleAmount> contain_articles;

    public Product() {}

    public Product(String name, List<ArticleAmount> contain_articles) {
        this.name = name;
        this.contain_articles = contain_articles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ArticleAmount> getContain_articles() {
        return contain_articles;
    }

    public void setContain_articles(List<ArticleAmount> contain_articles) {
        this.contain_articles = contain_articles;
    }
}

