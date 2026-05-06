package com.example.csit228capstone.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class EvacuationCenter {
    private UUID id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private int maxCapacity;
    private int currentOccupancy;
    private boolean isActive;
    private LocalDateTime createdAt;
    private String managerName;   // maps to manager_of_center in DB
    private String contactNumber;

    public EvacuationCenter() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public int getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(int maxCapacity) { this.maxCapacity = maxCapacity; }

    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }

    public String getStatus() {
        if(!isActive) return "Inactive";
        if (maxCapacity > 0 && currentOccupancy >= maxCapacity) {
            return "Full";
        }
        return "Available";
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }


    public double getOccupancyPercent() {
        return maxCapacity > 0 ? (double) currentOccupancy / maxCapacity * 100 : 0;
    }
}