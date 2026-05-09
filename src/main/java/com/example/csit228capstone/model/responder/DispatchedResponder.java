package com.example.csit228capstone.model.responder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DispatchedResponder {

    private UUID id;
    private UUID responderId;
    private UUID incidentId;

    private String severity;
    private LocalDateTime timeOccurred;
    private LocalDateTime dispatchedAt;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String status; // "dispatched", "returned", "cancelled"

    // ─── Joined / denormalized fields for display ─────────────────────────────
    private String responderName;
    private String responderAgency;
    private String incidentTitle; // from incidents table if available

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm");

    // ─── Constructors ────────────────────────────────────────────────────────────

    public DispatchedResponder() {}

    // ─── Getters & Setters ───────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getResponderId() { return responderId; }
    public void setResponderId(UUID responderId) { this.responderId = responderId; }

    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public LocalDateTime getTimeOccurred() { return timeOccurred; }
    public void setTimeOccurred(LocalDateTime timeOccurred) { this.timeOccurred = timeOccurred; }

    public LocalDateTime getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(LocalDateTime dispatchedAt) { this.dispatchedAt = dispatchedAt; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResponderName() { return responderName; }
    public void setResponderName(String responderName) { this.responderName = responderName; }

    public String getResponderAgency() { return responderAgency; }
    public void setResponderAgency(String responderAgency) { this.responderAgency = responderAgency; }

    public String getIncidentTitle() { return incidentTitle; }
    public void setIncidentTitle(String incidentTitle) { this.incidentTitle = incidentTitle; }

    // ─── Computed / display helpers ──────────────────────────────────────────────

    public String getShortId() {
        return id != null ? id.toString().substring(0, 8).toUpperCase() : "—";
    }

    public String getShortIncidentId() {
        return incidentId != null ? incidentId.toString().substring(0, 8).toUpperCase() : "—";
    }

    public String getFormattedDispatchTime() {
        return dispatchedAt != null ? dispatchedAt.format(DISPLAY_FMT) : "—";
    }

    public String getDeploymentSite() {
        if (latitude != null && longitude != null) {
            return String.format("%.4f, %.4f", latitude, longitude);
        }
        return "—";
    }

    /**
     * Elapsed time since dispatch (for active) or a fixed label (for returned/cancelled).
     * e.g. "2h 14m" or "Completed" or "Cancelled"
     */
    public String getDurationLabel() {
        if ("returned".equalsIgnoreCase(status))   return "Completed";
        if ("cancelled".equalsIgnoreCase(status))  return "Cancelled";
        if (dispatchedAt == null) return "—";

        Duration d = Duration.between(dispatchedAt, LocalDateTime.now());
        long hours = d.toHours();
        long mins  = d.toMinutesPart();
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    public String getStatusLabel() {
        if (status == null) return "Unknown";
        return switch (status.toLowerCase()) {
            case "dispatched" -> "Dispatched";
            case "returned"   -> "Returned";
            case "cancelled"  -> "Cancelled";
            default           -> status;
        };
    }
}