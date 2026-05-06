package com.example.csit228capstone.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class Resource {
    private UUID id;
    private String name;
    private String category;
    private double totalQty;
    private double availableQty;
    private double warningLevel;
    private String unit;
    private LocalDateTime updatedAt;

    public Resource() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getTotalQty() { return totalQty; }
    public void setTotalQty(double totalQty) { this.totalQty = totalQty; }
    public double getAvailableQty() { return availableQty; }
    public void setAvailableQty(double availableQty) { this.availableQty = availableQty; }
    public double getWarningLevel() { return warningLevel; }
    public void setWarningLevel(double warningLevel) { this.warningLevel = warningLevel; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public double getAvailablePercent() {
        return totalQty > 0 ? availableQty / totalQty * 100 : 0;
    }
}
