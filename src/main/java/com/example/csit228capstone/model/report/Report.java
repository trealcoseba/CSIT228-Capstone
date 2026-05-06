package com.example.csit228capstone.model.report;

public class Report {
    private String name;
    private String type;
    private String date;
    private String generatedBy;
    private String status;

    public Report(String name, String type, String date, String generatedBy, String status) {
        this.name = name;
        this.type = type;
        this.date = date;
        this.generatedBy = generatedBy;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public String getStatus() {
        return status;
    }
}
