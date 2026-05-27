package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic, format-agnostic data container for any report type.
 *
 * <T> is the typed payload:
 *   - {@code List<List<String>>} for tabular reports (Residents, Resources, etc.)
 *   - {@code ReportFormData}     for Incident Reports
 *
 * All exporters receive a {@code ReportData<T>} and extract the payload
 * via {@link #getPayload()} according to their concrete T.
 */
public class ReportData<T> {

    // ── Core Identity ────────────────────────────────────────────────────────
    private String        title;
    private ReportType    reportType;
    private LocalDateTime generatedDateTime; // Actual timestamp from the system

    // ── Metadata Fields (Used by Tabular/Generic Reports) ────────────────────
    private String        reportNo;
    private String        generatedBy;         // "Reported by"
    private String        recordedBy;
    private String        reporterContactInfo;
    private String        recorderContactInfo;
    private LocalDate     startDate;
    private LocalDate     endDate;

    // ── Table Structure ──────────────────────────────────────────────────────
    /** Column headers — used for tabular-type reports. */
    private List<String>  headers;

    /** One-line summary stats shown above the data table. */
    private List<String>  summaryLines;

    /**
     * The strongly-typed payload.
     * For tabular reports : {@code List<List<String>>}
     * For incident reports: {@code ReportFormData}
     */
    private T payload;

    /**
     * Backup reference to form context if needed.
     */
    private ReportFormData formData;

    public ReportData() {}

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType v) { this.reportType = v; }

    public LocalDateTime getGeneratedDateTime() { return generatedDateTime; }
    public void setGeneratedDateTime(LocalDateTime v) { this.generatedDateTime = v; }

    public String getReportNo() { return reportNo; }
    public void setReportNo(String v) { this.reportNo = v; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String v) { this.generatedBy = v; }

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

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> v) { this.headers = v; }

    public List<String> getSummaryLines() { return summaryLines; }
    public void setSummaryLines(List<String> v) { this.summaryLines = v; }

    public T getPayload() { return payload; }
    public void setPayload(T v) { this.payload = v; }

    public ReportFormData getFormData() { return formData; }
    public void setFormData(ReportFormData v) { this.formData = v; }

    @SuppressWarnings("unchecked")
    public List<List<String>> getRows() {
        if (payload instanceof List) {
            return (List<List<String>>) payload;
        }
        return null;
    }
}