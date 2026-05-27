package com.example.csit228capstone.model.emergency;

import com.example.csit228capstone.model.incident.IncidentSeverity;
import com.example.csit228capstone.model.incident.IncidentType;

import java.util.UUID;

public class EmergencyContext {

    private UUID    incidentId;
    private IncidentType   type;
    private IncidentSeverity severity;
    private String  title;
    private String  description;
    private double  latitude;
    private double  longitude;
    private double  radiusMeters;
    private String  locationPurok;
    private String  locationDetail;
    private UUID    reportedBy;

    public EmergencyContext() {}

    public UUID getIncidentId()          { return incidentId; }

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

    public String getLocationDetail()    { return locationDetail; }
    public void setLocationDetail(String v){ this.locationDetail = v; }

    public UUID getReportedBy()          { return reportedBy; }
    public void setReportedBy(UUID v)    { this.reportedBy = v; }
}
