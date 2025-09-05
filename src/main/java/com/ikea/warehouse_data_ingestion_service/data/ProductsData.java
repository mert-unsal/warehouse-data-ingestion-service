package com.ikea.warehouse_data_ingestion_service.data;

import java.util.List;

public class ProductsData {
    private List<Product> products;

    public ProductsData() {}

    public ProductsData(List<Product> products) {
        this.products = products;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}

