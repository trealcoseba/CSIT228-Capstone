package com.example.csit228capstone.model.document;

public enum DocumentType {
    BARANGAY_CLEARANCE("Barangay Clearance"),
    CERTIFICATE_OF_INDIGENCY("Certificate of Indigency"),
    CERTIFICATE_OF_RESIDENCY("Certificate of Residency"),
    BLOTTER_REPORT("Blotter Report"),
    BUSINESS_PERMIT("Business Permit"),
    OTHER("Other");

    private final String displayName;
    DocumentType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
