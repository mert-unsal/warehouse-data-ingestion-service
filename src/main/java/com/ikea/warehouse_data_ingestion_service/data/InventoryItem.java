package com.ikea.warehouse_data_ingestion_service.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InventoryItem {
    @JsonProperty("art_id")
    private String art_id; // Keeping original field name for clarity; can map later if needed
    private String name;
    private String stock;

    public InventoryItem() {}

    public InventoryItem(String art_id, String name, String stock) {
        this.art_id = art_id;
        this.name = name;
        this.stock = stock;
    }

    public String getArt_id() {
        return art_id;
    }

    public void setArt_id(String art_id) {
        this.art_id = art_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }
}
