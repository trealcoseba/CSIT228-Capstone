package com.example.csit228capstone.model.chatbot;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatbotMessage {
    private UUID id;
    private UUID sessionId;
    private String role; // "user", "assistant", "admin"
    private String content;
    private LocalDateTime createdAt;

    public ChatbotMessage() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
