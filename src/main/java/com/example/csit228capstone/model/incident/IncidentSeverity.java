package com.example.csit228capstone.model.incident;

public enum IncidentSeverity {
    CRITICAL("Critical", "#C0392B"),
    MAJOR("Major", "#BA7517"),
    MINOR("Minor", "#3498DB");

    private final String displayName;
    private final String color;
    IncidentSeverity(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
}
