package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

/** Observable row for the Rescue Teams TableView (backed by Responder data). */
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
        return switch (s.toLowerCase()) {
            case "available"   -> "Available";
            case "dispatched"  -> "Dispatched";
            case "unavailable" -> "Unavailable";
            default            -> s;
        };
    }

    public StringProperty teamNameProperty()     { return teamName; }
    public StringProperty availabilityProperty() { return availability; }
    public StringProperty vehicleProperty()      { return vehicle; }
    public StringProperty etaProperty()          { return eta; }

    public String getTeamName()     { return teamName.get(); }
    public String getAvailability() { return availability.get(); }
    public String getVehicle()      { return vehicle.get(); }
    public String getEta()          { return eta.get(); }

    public void setAvailability(String v) { availability.set(v); }
    public void setEta(String v)          { eta.set(v); }

    public UUID getResponderId() { return responderId; }
}
