package com.example.csit228capstone.model.incident;

import java.time.LocalDateTime;
import java.util.UUID;

public class IncidentTimelineEntry {
    private UUID id;
    private UUID incidentId;
    private IncidentStatus oldStatus;
    private IncidentStatus newStatus;
    private UUID changedBy;
    private String note;
    private LocalDateTime changedAt;

    public IncidentTimelineEntry() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getIncidentId() { return incidentId; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
    public IncidentStatus getOldStatus() { return oldStatus; }
    public void setOldStatus(IncidentStatus oldStatus) { this.oldStatus = oldStatus; }
    public IncidentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(IncidentStatus newStatus) { this.newStatus = newStatus; }
    public UUID getChangedBy() { return changedBy; }
    public void setChangedBy(UUID changedBy) { this.changedBy = changedBy; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
