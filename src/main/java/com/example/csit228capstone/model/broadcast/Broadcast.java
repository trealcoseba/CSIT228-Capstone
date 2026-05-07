package com.example.csit228capstone.model.broadcast;

import java.time.LocalDateTime;

public class Broadcast {
    private int id;
    private String title;
    private String message;
    private String category;
    private LocalDateTime scheduledDate;

    public Broadcast(int id, String title, String message, String category, LocalDateTime scheduledDate) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.scheduledDate = scheduledDate;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getCategory() { return category; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
}