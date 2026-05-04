package com.example.csit228capstone.model.document;

public enum DocumentStatus {
    PENDING("Pending"), PROCESSING("Processing"),
    READY("Ready"), RELEASED("Released"), REJECTED("Rejected");

    private final String displayName;
    DocumentStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
