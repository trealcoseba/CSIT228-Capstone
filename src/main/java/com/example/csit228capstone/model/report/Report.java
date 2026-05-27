package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class Report {

    private UUID          id;
    private String        name;
    private String        type;
    private String        reportNo;
    private LocalDate     dateOfReport;
    private String        generatedBy;
    private String        recordedBy;
    private String        reporterContact;
    private String        recorderContact;
    private LocalDateTime generatedDateTime;
    private LocalDate     startDate;
    private LocalDate     endDate;
    private LocalDate     dateOfIncident;
    private String        location;
    private String        description;
    private Boolean       hasInsurance;
    private String        insurancePolicy;
    private String        insuranceCoverageAmt;
    private String        incidentTypeOther;
    private boolean       isSelected;

    public Report() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }

    public LocalDate getDateOfReport() { return dateOfReport; }
    public void setDateOfReport(LocalDate dateOfReport) { this.dateOfReport = dateOfReport; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public String getReporterContact() { return reporterContact; }
    public void setReporterContact(String reporterContact) { this.reporterContact = reporterContact; }

    public String getRecorderContact() { return recorderContact; }
    public void setRecorderContact(String recorderContact) { this.recorderContact = recorderContact; }

    public LocalDateTime getGeneratedDateTime() { return generatedDateTime; }
    public void setGeneratedDateTime(LocalDateTime generatedDateTime) { this.generatedDateTime = generatedDateTime; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getDateOfIncident() { return dateOfIncident; }
    public void setDateOfIncident(LocalDate dateOfIncident) { this.dateOfIncident = dateOfIncident; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getHasInsurance() { return hasInsurance; }
    public void setHasInsurance(Boolean hasInsurance) { this.hasInsurance = hasInsurance; }

    public String getInsurancePolicy() { return insurancePolicy; }
    public void setInsurancePolicy(String insurancePolicy) { this.insurancePolicy = insurancePolicy; }

    public String getInsuranceCoverageAmt() { return insuranceCoverageAmt; }
    public void setInsuranceCoverageAmt(String insuranceCoverageAmt) { this.insuranceCoverageAmt = insuranceCoverageAmt; }

    public String getIncidentTypeOther() { return incidentTypeOther; }
    public void setIncidentTypeOther(String incidentTypeOther) { this.incidentTypeOther = incidentTypeOther; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }

    public String getFormattedDate() {
        if (generatedDateTime == null) return "";
        return generatedDateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }
}