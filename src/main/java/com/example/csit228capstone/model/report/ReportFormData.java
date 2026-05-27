package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Carries every field collected from ReportForm.fxml.
 * Only the fields relevant to the chosen report type are filled.
 * This object is passed from ReportFormController → ReportPreviewController
 * → PdfReportExporter / DocxReportExporter.
 */
public class ReportFormData {

    // ── Universal (all report types) ─────────────────────────────────────────
    private String    reportNo;
    private LocalDate date;
    private String    reportedBy;
    private String    recordedBy;
    private String    reporterContactInfo;
    private String    recorderContactInfo;
    private LocalDate startDate;
    private LocalDate endDate;

    // ── Incident Report specific ──────────────────────────────────────────────
    private LocalDate    dateOfIncident;
    private String       location;
    private String       description;
    private List<String> incidentTypes      = new ArrayList<>();
    private String       incidentTypeOther;
    private Boolean      hasInsurance;          // null = not answered
    private String       insurancePolicy;
    private String       insuranceCoverageAmt;
    private List<DamageRow>  damages  = new ArrayList<>();
    private List<InjuryRow>  injuries = new ArrayList<>();

    // ── Inner row types ───────────────────────────────────────────────────────

    public static class DamageRow {
        public String damage = "", value = "", repairPlan = "", repairCost = "";
        public DamageRow() {}
        public DamageRow(String d, String v, String rp, String rc) {
            damage = d; value = v; repairPlan = rp; repairCost = rc;
        }
    }

    public static class InjuryRow {
        public String injuredPerson = "", position = "", medicalCost = "", insurance = "";
        public InjuryRow() {}
        public InjuryRow(String ip, String pos, String mc, String ins) {
            injuredPerson = ip; position = pos; medicalCost = mc; insurance = ins;
        }
    }

    // ── Getters & setters ─────────────────────────────────────────────────────

    public String getReportNo() { return reportNo; }
    public void setReportNo(String v) { this.reportNo = v; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }

    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String v) { this.reportedBy = v; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String v) { this.recordedBy = v; }

    public String getReporterContactInfo() { return reporterContactInfo; }
    public void setReporterContactInfo(String v) { this.reporterContactInfo = v; }

    public String getRecorderContactInfo() { return recorderContactInfo; }
    public void setRecorderContactInfo(String v) { this.recorderContactInfo = v; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }

    public LocalDate getDateOfIncident() { return dateOfIncident; }
    public void setDateOfIncident(LocalDate v) { this.dateOfIncident = v; }

    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }

    public List<String> getIncidentTypes() { return incidentTypes; }
    public void setIncidentTypes(List<String> v) { this.incidentTypes = v; }

    public String getIncidentTypeOther() { return incidentTypeOther; }
    public void setIncidentTypeOther(String v) { this.incidentTypeOther = v; }

    public Boolean getHasInsurance() { return hasInsurance; }
    public void setHasInsurance(Boolean v) { this.hasInsurance = v; }

    public String getInsurancePolicy() { return insurancePolicy; }
    public void setInsurancePolicy(String v) { this.insurancePolicy = v; }

    public String getInsuranceCoverageAmt() { return insuranceCoverageAmt; }
    public void setInsuranceCoverageAmt(String v) { this.insuranceCoverageAmt = v; }

    public List<DamageRow> getDamages() { return damages; }
    public void setDamages(List<DamageRow> v) { this.damages = v; }

    public List<InjuryRow> getInjuries() { return injuries; }
    public void setInjuries(List<InjuryRow> v) { this.injuries = v; }
}