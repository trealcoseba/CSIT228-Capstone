package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.util.List;

/**
 * A format-agnostic data container that holds all content needed
 * to render a report in any export format (PDF, Excel, CSV).
 *
 * The exporters (PdfReportExporter, ExcelReportExporter, CsvReportExporter)
 * consume this object — they never touch repositories directly.
 */
public class ReportData {

    private String title;
    private String generatedBy;
    private LocalDate startDate;
    private LocalDate endDate;
    private ReportType reportType;

    /** Column headers for the data table. */
    private List<String> headers;

    /**
     * Each inner list is one row. Values are plain strings so
     * all exporters can handle them uniformly.
     */
    private List<List<String>> rows;

    /** Optional summary stats shown at the top of the report. */
    private List<String> summaryLines;

    public ReportData() {}

    // ── Getters & setters ─────────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }

    public List<List<String>> getRows() { return rows; }
    public void setRows(List<List<String>> rows) { this.rows = rows; }

    public List<String> getSummaryLines() { return summaryLines; }
    public void setSummaryLines(List<String> summaryLines) { this.summaryLines = summaryLines; }
}