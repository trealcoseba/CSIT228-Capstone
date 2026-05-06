package com.example.csit228capstone.model.incident;

public enum IncidentType {
    FLOOD("Flood"), FIRE("Fire"), MEDICAL_EMERGENCY("Medical Emergency"),
    MISSING_PERSON("Missing Person"), EARTHQUAKE("Earthquake"),
    TYPHOON("Typhoon"), LANDSLIDE("Landslide"), OTHER("Other");

    private final String displayName;
    IncidentType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
