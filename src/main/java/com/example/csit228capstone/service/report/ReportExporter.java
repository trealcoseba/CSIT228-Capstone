package com.example.csit228capstone.service.report;

import com.example.csit228capstone.model.report.ReportData;
import java.io.File;

/**
 * Generic exporter contract.
 *
 * <p>{@code T} is the payload type stored inside {@link ReportData}:</p>
 * <ul>
 *   <li>{@code List<List<String>>} — tabular reports (Residents, Resources, etc.)</li>
 *   <li>{@code ReportFormData}     — Natural Disaster Incident Report</li>
 * </ul>
 *
 * <p>{@link com.example.csit228capstone.service.ReportService} stores exporters
 * as {@code ReportExporter<?>} (wildcard) and routes each call to the correct
 * concrete exporter without unchecked casts at the call site.</p>
 */
public interface ReportExporter<T> {

    /**
     * Writes the report described by {@code data} to {@code destination}.
     *
     * @param data        fully-populated report data container
     * @param destination target file (PDF, DOCX, etc.)
     * @throws Exception  any I/O or rendering error
     */
    void export(ReportData<T> data, File destination) throws Exception;

    /** Human-readable format label, e.g. {@code "PDF"} or {@code "DOCX"}. */
    String getFormatName();
}