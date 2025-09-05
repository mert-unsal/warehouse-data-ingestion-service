package com.ikea.warehouse_data_ingestion_service.data;

import java.util.List;

public class InventoryData {
    private List<InventoryItem> inventory;

    public InventoryData() {}

    public InventoryData(List<InventoryItem> inventory) {
        this.inventory = inventory;
    }

    public List<InventoryItem> getInventory() {
        return inventory;
    }

    public void setInventory(List<InventoryItem> inventory) {
        this.inventory = inventory;
    }
}

