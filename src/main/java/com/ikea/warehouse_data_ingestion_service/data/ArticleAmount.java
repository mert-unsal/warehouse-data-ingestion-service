package com.ikea.warehouse_data_ingestion_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArticleAmount {
    @JsonProperty("art_id")
    private String art_id;
    @JsonProperty("amount_of")
    private String amount_of;

    public ArticleAmount() {}

    public ArticleAmount(String art_id, String amount_of) {
        this.art_id = art_id;
        this.amount_of = amount_of;
    }

    public String getArt_id() {
        return art_id;
    }

    public void setArt_id(String art_id) {
        this.art_id = art_id;
    }

    public String getAmount_of() {
        return amount_of;
    }

    public void setAmount_of(String amount_of) {
        this.amount_of = amount_of;
    }
}

