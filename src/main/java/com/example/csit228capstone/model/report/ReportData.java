package com.example.csit228capstone.model.report;

import java.time.LocalDate;
import java.util.List;

/**
 * Generic, format-agnostic data container for any report type.
 *
 * <T> is the typed payload:
 *   - {@code List<List<String>>} for tabular reports (Residents, Resources, etc.)
 *   - {@code ReportFormData}     for Natural Disaster Incident Report
 *
 * All exporters receive a {@code ReportData<T>} and extract the payload
 * via {@link #getPayload()} according to their concrete T.
 *
 * Usage examples:
 * <pre>
 *   // Tabular report
 *   ReportData&lt;List&lt;List&lt;String&gt;&gt;&gt; tabular = new ReportData&lt;&gt;();
 *   tabular.setPayload(rows);
 *
 *   // Incident report
 *   ReportData&lt;ReportFormData&gt; incident = new ReportData&lt;&gt;();
 *   incident.setPayload(formData);
 * </pre>
 */
public class ReportData<T> {

    private String     title;
    private String     generatedBy;
    private LocalDate  startDate;
    private LocalDate  endDate;
    private ReportType reportType;

    /** Column headers — used for tabular-type reports. */
    private List<String> headers;

    /** One-line summary stats shown above the data table. */
    private List<String> summaryLines;

    /**
     * The strongly-typed payload.
     * For tabular reports : {@code List<List<String>>} (rows × columns of strings)
     * For incident reports: {@code ReportFormData}
     */
    private T payload;

    public ReportData() {}

    // ── Getters & setters ─────────────────────────────────────────────────────

    public String getTitle()                         { return title; }
    public void   setTitle(String v)                 { this.title = v; }

    public String getGeneratedBy()                   { return generatedBy; }
    public void   setGeneratedBy(String v)           { this.generatedBy = v; }

    public LocalDate getStartDate()                  { return startDate; }
    public void      setStartDate(LocalDate v)       { this.startDate = v; }

    public LocalDate getEndDate()                    { return endDate; }
    public void      setEndDate(LocalDate v)         { this.endDate = v; }

    public ReportType getReportType()                { return reportType; }
    public void       setReportType(ReportType v)    { this.reportType = v; }

    public List<String> getHeaders()                 { return headers; }
    public void         setHeaders(List<String> v)   { this.headers = v; }

    public List<String> getSummaryLines()                { return summaryLines; }
    public void         setSummaryLines(List<String> v)  { this.summaryLines = v; }

    public T    getPayload()           { return payload; }
    public void setPayload(T payload)  { this.payload = payload; }

    // ── Convenience — only valid when T = List<List<String>> ─────────────────

    /**
     * Returns the payload cast to {@code List<List<String>>}.
     * Only call this when you know the payload is tabular.
     *
     * @throws ClassCastException if the payload is not a List
     */
    @SuppressWarnings("unchecked")
    public List<List<String>> getRows() {
        return (List<List<String>>) payload;
    }
}