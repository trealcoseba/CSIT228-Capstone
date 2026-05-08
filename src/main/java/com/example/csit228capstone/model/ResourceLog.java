package com.example.csit228capstone.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ResourceLog {
    private UUID id;
    private UUID resourceId;
    private String resourceName;
    private String resourceUnit;
    private UUID evacuationCenterId;
    private String evacuationCenterName;
    private String purpose;
    private LocalDateTime dateUsed;
    private double quantityUsed;
    private double quantityAvailableAtTime;
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getResourceUnit() { return resourceUnit; }
    public void setResourceUnit(String resourceUnit) { this.resourceUnit = resourceUnit; }

    public UUID getEvacuationCenterId() { return evacuationCenterId; }
    public void setEvacuationCenterId(UUID evacuationCenterId) { this.evacuationCenterId = evacuationCenterId; }

    public String getEvacuationCenterName() { return evacuationCenterName; }
    public void setEvacuationCenterName(String evacuationCenterName) { this.evacuationCenterName = evacuationCenterName; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public LocalDateTime getDateUsed() { return dateUsed; }
    public void setDateUsed(LocalDateTime dateUsed) { this.dateUsed = dateUsed; }

    public double getQuantityUsed() { return quantityUsed; }
    public void setQuantityUsed(double quantityUsed) { this.quantityUsed = quantityUsed; }

    public double getQuantityAvailableAtTime() { return quantityAvailableAtTime; }
    public void setQuantityAvailableAtTime(double quantityAvailableAtTime) { this.quantityAvailableAtTime = quantityAvailableAtTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
