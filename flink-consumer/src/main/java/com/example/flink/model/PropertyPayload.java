package com.example.flink.model;

import java.io.Serializable;

/**
 * Domain object representing the payload of a property event.
 * This is Flink-safe (POJO + Serializable).
 */
public class PropertyPayload implements Serializable {

    private String propertyId;
    private Double price;
    private String status;

    // Required empty constructor for Flink POJO
    public PropertyPayload() {
    }

    public PropertyPayload(String propertyId, Double price, String status) {
        this.propertyId = propertyId;
        this.price = price;
        this.status = status;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public Double getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "PropertyPayload{" +
                "propertyId='" + propertyId + '\'' +
                ", price=" + price +
                ", status='" + status + '\'' +
                '}';
    }
}
