package com.example.csit228capstone.model.emergency;

import com.example.csit228capstone.model.incident.IncidentSeverity;
import com.example.csit228capstone.model.incident.IncidentType;

import java.util.UUID;

/**
 * Lightweight data-carrier passed from the "Trigger Incident" dialog
 * into EmergencyController via scene userData or a static setter.
 */
public class EmergencyContext {

    private UUID    incidentId;
    private IncidentType   type;
    private IncidentSeverity severity;
    private String  title;
    private String  description;
    private double  latitude;
    private double  longitude;
    private double  radiusMeters;   // affected radius the admin entered
    private String  locationPurok;
    private String  locationDetail;
    private UUID    reportedBy;     // admin user UUID (may be null)

    public EmergencyContext() {}

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public UUID getIncidentId()          { return incidentId; }
    public void setIncidentId(UUID v)    { this.incidentId = v; }

    public IncidentType getType()        { return type; }
    public void setType(IncidentType v)  { this.type = v; }

    public IncidentSeverity getSeverity()          { return severity; }
    public void setSeverity(IncidentSeverity v)    { this.severity = v; }

    public String getTitle()             { return title; }
    public void setTitle(String v)       { this.title = v; }

    public String getDescription()       { return description; }
    public void setDescription(String v) { this.description = v; }

    public double getLatitude()          { return latitude; }
    public void setLatitude(double v)    { this.latitude = v; }

    public double getLongitude()         { return longitude; }
    public void setLongitude(double v)   { this.longitude = v; }

    public double getRadiusMeters()      { return radiusMeters; }
    public void setRadiusMeters(double v){ this.radiusMeters = v; }

    public String getLocationPurok()     { return locationPurok; }
    public void setLocationPurok(String v){ this.locationPurok = v; }

    public String getLocationDetail()    { return locationDetail; }
    public void setLocationDetail(String v){ this.locationDetail = v; }

    public UUID getReportedBy()          { return reportedBy; }
    public void setReportedBy(UUID v)    { this.reportedBy = v; }
}
