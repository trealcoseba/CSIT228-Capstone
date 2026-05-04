package com.example.csit228capstone.model.chatbot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ChatbotSession {
    private UUID id;
    private UUID residentId;
    private ChatbotCategory category;
    private boolean isResolved;
    private boolean escalated;
    private LocalDateTime createdAt;
    private List<ChatbotMessage> messages;
    private String residentName; // transient

    public ChatbotSession() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getResidentId() { return residentId; }
    public void setResidentId(UUID residentId) { this.residentId = residentId; }
    public ChatbotCategory getCategory() { return category; }
    public void setCategory(ChatbotCategory category) { this.category = category; }
    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { isResolved = resolved; }
    public boolean isEscalated() { return escalated; }
    public void setEscalated(boolean escalated) { this.escalated = escalated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<ChatbotMessage> getMessages() { return messages; }
    public void setMessages(List<ChatbotMessage> messages) { this.messages = messages; }
    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }
}
