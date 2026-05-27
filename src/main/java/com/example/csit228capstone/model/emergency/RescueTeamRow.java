package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

public class RescueTeamRow {

    private final UUID responderId;

    private final StringProperty teamName     = new SimpleStringProperty();
    private final StringProperty availability = new SimpleStringProperty();
    private final StringProperty vehicle      = new SimpleStringProperty();
    private final StringProperty eta          = new SimpleStringProperty();

    public RescueTeamRow(UUID id, String name, String status, String agency, String etaStr) {
        this.responderId = id;
        teamName.set(name);
        availability.set(formatStatus(status));
        vehicle.set(agency != null ? agency : "—");
        eta.set(etaStr != null ? etaStr : "—");
    }

    private String formatStatus(String s) {
        if (s == null) return "Unknown";

        String normalized = s.toLowerCase().replace("_", " ");

        return switch (normalized) {
            case "available"               -> "Available";
            case "dispatched", "on mission" -> "Dispatched";
            case "unavailable", "off duty" -> "Unavailable";
            default                        -> s;
        };
    }

    public StringProperty teamNameProperty()     { return teamName; }
    public StringProperty availabilityProperty() { return availability; }
    public StringProperty vehicleProperty()      { return vehicle; }
    public StringProperty etaProperty()          { return eta; }
    public String getTeamName()     { return teamName.get(); }
    public String getAvailability() { return availability.get(); }
    public void setAvailability(String v) { availability.set(v); }
    public UUID getResponderId() { return responderId; }
}
