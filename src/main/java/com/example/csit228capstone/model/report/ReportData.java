package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ReportData<T> {

    private String        title;
    private ReportType    reportType;
    private LocalDateTime generatedDateTime;

    private String        reportNo;
    private String        generatedBy;
    private String        recordedBy;
    private String        reporterContactInfo;
    private String        recorderContactInfo;
    private LocalDate     startDate;
    private LocalDate     endDate;

    private List<String>  headers;

    private List<String>  summaryLines;

    private T payload;

    private ReportFormData formData;

    public ReportData() {}

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