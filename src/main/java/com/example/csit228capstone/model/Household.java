package com.example.csit228capstone.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Household {
    private UUID id;
    private String householdNumber;
    private String purok;
    private String address;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private List<Resident> members;

    public Household() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getHouseholdNumber() { return householdNumber; }
    public void setHouseholdNumber(String householdNumber) { this.householdNumber = householdNumber; }
    public String getPurok() { return purok; }
    public void setPurok(String purok) { this.purok = purok; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<Resident> getMembers() { return members; }
    public void setMembers(List<Resident> members) { this.members = members; }
}
