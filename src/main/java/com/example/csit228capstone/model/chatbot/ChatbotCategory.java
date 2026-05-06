package com.example.csit228capstone.model.chatbot;

public enum ChatbotCategory {
    DOCUMENT_REQUEST("Document Request"),
    GENERAL_INQUIRY("General Inquiry"),
    EMERGENCY("Emergency");

    private final String displayName;
    ChatbotCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
