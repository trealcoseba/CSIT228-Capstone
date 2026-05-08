package com.example.csit228capstone.model.alert;

public enum AlertPriority {
    INFO("Info", "#3498DB"),
    WARNING("Warning", "#BA7517"),
    CRITICAL("Critical", "#C0392B");

    private final String displayName;
    private final String color;
    AlertPriority(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
}
