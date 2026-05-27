package com.example.csit228capstone.model.responder;

import java.time.LocalDateTime;
import java.util.UUID;

public class Responder {

    private UUID id;
    private String name;
    private String contact;
    private String agency;
    private String status;
    private LocalDateTime createdAt;

    private int totalDispatches;
    private int activeDispatches;

    public Responder() {}

    public Responder(String name, String contact, String agency, String status) {
        this.name = name;
        this.contact = contact;
        this.agency = agency;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getAgency() { return agency; }
    public void setAgency(String agency) { this.agency = agency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getTotalDispatches() { return totalDispatches; }
    public void setTotalDispatches(int totalDispatches) { this.totalDispatches = totalDispatches; }

    public int getActiveDispatches() { return activeDispatches; }
    public void setActiveDispatches(int activeDispatches) { this.activeDispatches = activeDispatches; }

    public String getShortId() {
        return id != null ? id.toString().substring(0, 8).toUpperCase() : "—";
    }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        return switch (status.toLowerCase()) {
            case "available"  -> "Available";
            case "on_mission" -> "On Mission";
            case "off_duty"   -> "Off Duty";
            default           -> status;
        };
    }

    @Override
    public String toString() {
        return name + " (" + agency + ")";
    }
}