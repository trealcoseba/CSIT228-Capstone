package com.example.csit228capstone.model.incident;

public enum IncidentStatus {
    REPORTED("Reported"), DISPATCHED("Dispatched"),
    RESPONDING("Responding"), MONITORING("Monitoring"), RESOLVED("Resolved");

    private final String displayName;
    IncidentStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
