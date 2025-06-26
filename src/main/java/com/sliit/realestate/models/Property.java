package com.sliit.realestate.models;


import java.math.BigDecimal; // Use BigDecimal for currency
import java.util.UUID;

public class Property {

    private String propertyId;
    private String agentId; // ID of the agent managing this property
    private String address;
    private BigDecimal price;
    private String type; // e.g., HOUSE, APARTMENT, LAND
    private PropertyStatus status; // e.g., FOR_SALE, SOLD, PENDING
    private String description;
    private String imageUrl; // URL to an image
    private int bedrooms;
    private int bathrooms;
    private double areaSqFt; // Area in square feet or meters

    public enum PropertyStatus {
        FOR_SALE, FOR_RENT, PENDING, SOLD, RENTED, DRAFT
    }

    // Default constructor (useful for some frameworks)
    public Property() {
        this.status = PropertyStatus.DRAFT; // Default status
    }

    // Constructor for creating a new property (ID generated)
    public Property(String agentId, String address, BigDecimal price, String type, String description, String imageUrl, int bedrooms, int bathrooms, double areaSqFt) {
        this.propertyId = UUID.randomUUID().toString();
        this.agentId = agentId;
        this.address = address;
        this.price = price;
        this.type = type;
        this.status = PropertyStatus.FOR_SALE; // Default to FOR_SALE when created this way
        this.description = description;
        this.imageUrl = imageUrl;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.areaSqFt = areaSqFt;
    }

    // Full constructor (useful for loading from file/repository)
    public Property(String propertyId, String agentId, String address, BigDecimal price, String type, PropertyStatus status, String description, String imageUrl, int bedrooms, int bathrooms, double areaSqFt) {
        this.propertyId = propertyId;
        this.agentId = agentId;
        this.address = address;
        this.price = price;
        this.type = type;
        this.status = status;
        this.description = description;
        this.imageUrl = imageUrl;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.areaSqFt = areaSqFt;
    }


    // --- Getters ---
    public String getPropertyId() { return propertyId; }
    public String getAgentId() { return agentId; }
    public String getAddress() { return address; }
    public BigDecimal getPrice() { return price; }
    public String getType() { return type; }
    public PropertyStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getBedrooms() { return bedrooms; }
    public int getBathrooms() { return bathrooms; }
    public double getAreaSqFt() { return areaSqFt; }

    // --- Setters --- (Allow modification of properties)
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; } // Usually set only once
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setAddress(String address) { this.address = address; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setType(String type) { this.type = type; }
    public void setStatus(PropertyStatus status) { this.status = status; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }
    public void setAreaSqFt(double areaSqFt) { this.areaSqFt = areaSqFt; }

    @Override
    public String toString() {
        return "Property{" +
                "propertyId='" + propertyId + '\'' +
                ", agentId='" + agentId + '\'' +
                ", address='" + address + '\'' +
                ", price=" + price +
                ", status=" + status +
                '}';
    }
}