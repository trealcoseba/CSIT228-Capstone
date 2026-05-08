package com.example.csit228capstone.model.report;

public enum ReportType {

    RESIDENT_SUMMARY("Residents Report"),
    RESOURCE_INVENTORY("Resource Inventory"),
    INCIDENT_SUMMARY("Incident Summary"),
    EVACUATION_REPORT("Evacuation Report"),
    MONTHLY_SUMMARY("Monthly Summary"),
    CUSTOM_REPORT("Custom Report");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ReportType fromDisplayName(String name) {
        for (ReportType rt : values()) {
            if (rt.displayName.equalsIgnoreCase(name)) return rt;
        }
        return CUSTOM_REPORT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
