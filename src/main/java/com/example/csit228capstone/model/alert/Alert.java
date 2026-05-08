package com.example.csit228capstone.model.alert;

import java.time.LocalDateTime;
import java.util.UUID;

public class Alert {
    private UUID id;
    private String title;
    private String body;
    private AlertPriority priority;
    private boolean isBroadcast;
    private String targetPurok;
    private UUID issuedBy;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public Alert() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public AlertPriority getPriority() { return priority; }
    public void setPriority(AlertPriority priority) { this.priority = priority; }
    public boolean isBroadcast() { return isBroadcast; }
    public void setBroadcast(boolean broadcast) { isBroadcast = broadcast; }
    public String getTargetPurok() { return targetPurok; }
    public void setTargetPurok(String targetPurok) { this.targetPurok = targetPurok; }
    public UUID getIssuedBy() { return issuedBy; }
    public void setIssuedBy(UUID issuedBy) { this.issuedBy = issuedBy; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
