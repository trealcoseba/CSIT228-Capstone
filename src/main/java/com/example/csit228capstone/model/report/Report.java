package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Report {
    private UUID id;
    private String name;
    private String type;
    private LocalDateTime generatedDateTime;
    private String generatedBy;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isSelected;

    public Report() {}

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public LocalDateTime getGeneratedDateTime() {
        return generatedDateTime;
    }
    public void setGeneratedDateTime(LocalDateTime generatedDateTime) {
        this.generatedDateTime = generatedDateTime;
    }
    public String getGeneratedBy() {
        return generatedBy;
    }
    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
    public boolean isSelected() {
        return isSelected;
    }
    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return endDate;
    }
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return this.generatedDateTime.format(formatter);
    }
}