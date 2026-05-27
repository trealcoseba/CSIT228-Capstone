package com.example.csit228capstone.model.report;

/**
 * Enum for all supported report types.
 * Each entry carries a display name and a description shown in the form.
 */
public enum ReportType {

    RESIDENT_SUMMARY(
            "Residents Report",
            "A complete list of all registered barangay residents including demographic " +
                    "details such as age, sex, civil status, address, contact number, and any " +
                    "recorded vulnerabilities. Useful for census and social welfare assessments."),

    RESOURCE_INVENTORY(
            "Resource Inventory",
            "An overview of all barangay resources and supplies — food packs, medicines, " +
                    "rescue equipment, and more. Shows current quantities, warning thresholds, " +
                    "and items that are running low."),

    INCIDENT_REPORT(
            "Incident Report",
            "A detailed incident report for natural disasters such as floods, typhoons, " +
                    "earthquakes, and fires. Captures damages to property, injuries, insurance " +
                    "information, and disaster type classifications for official documentation."),

    EVACUATION_REPORT(
            "Evacuation Report",
            "Lists all residents eligible or registered for evacuation. Used during " +
                    "calamities to track evacuees, assign evacuation centers, and coordinate " +
                    "relief efforts with LGU and disaster response teams."),

    MONTHLY_SUMMARY(
            "Monthly Summary",
            "A high-level overview of barangay operations for the month: total residents, " +
                    "resource stock levels, number of incidents, and resolved vs. ongoing cases. " +
                    "Great for executive summaries and LGU reporting."),

    CUSTOM_REPORT(
            "Custom Report",
            "A flexible report type for special purposes not covered by the standard " +
                    "report categories. Configure manually or use for ad-hoc barangay needs.");

    private final String displayName;
    private final String description;

    ReportType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }

    public static ReportType fromDisplayName(String name) {
        if (name == null) return CUSTOM_REPORT;
        for (ReportType rt : values()) {
            if (rt.displayName.equalsIgnoreCase(name.trim())) return rt;
        }
        return CUSTOM_REPORT;
    }

    @Override
    public String toString() { return displayName; }
}