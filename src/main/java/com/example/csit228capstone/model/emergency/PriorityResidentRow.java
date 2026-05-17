package com.example.csit228capstone.model.emergency;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.UUID;

/**
 * Observable row for the Priority Residents TableView.
 * Priority = vulnerable residents first, then sorted by distance.
 */
public class PriorityResidentRow {

    private final UUID   residentId;
    private final boolean vulnerable;

    private final StringProperty residentName        = new SimpleStringProperty();
    private final StringProperty vulnerabilityType   = new SimpleStringProperty();
    private final StringProperty distanceFromIncident= new SimpleStringProperty();
    private final StringProperty rescueStatus        = new SimpleStringProperty();

    public PriorityResidentRow(UUID id, String name, String vulnType,
                               double distanceM, boolean isVulnerable) {
        this.residentId  = id;
        this.vulnerable  = isVulnerable;
        residentName.set(name);
        vulnerabilityType.set(vulnType.isBlank() ? "General" : vulnType);
        distanceFromIncident.set(String.format("%.0f m", distanceM));
        rescueStatus.set("Pending");
    }

    // ─── Property accessors (required by PropertyValueFactory) ───────────────

    public StringProperty residentNameProperty()         { return residentName; }
    public StringProperty vulnerabilityTypeProperty()    { return vulnerabilityType; }
    public StringProperty distanceFromIncidentProperty() { return distanceFromIncident; }
    public StringProperty rescueStatusProperty()         { return rescueStatus; }

    public String getResidentName()          { return residentName.get(); }
    public String getVulnerabilityType()     { return vulnerabilityType.get(); }
    public String getDistanceFromIncident()  { return distanceFromIncident.get(); }
    public String getRescueStatus()          { return rescueStatus.get(); }

    public void setRescueStatus(String v)    { rescueStatus.set(v); }

    // ─── Raw accessors ───────────────────────────────────────────────────────
    public UUID    getResidentId()  { return residentId; }
    public boolean isVulnerable()   { return vulnerable; }
}
